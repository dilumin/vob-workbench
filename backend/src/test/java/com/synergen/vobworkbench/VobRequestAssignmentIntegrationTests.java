package com.synergen.vobworkbench;

import static org.assertj.core.api.Assertions.assertThat;

import com.synergen.vobworkbench.model.CoverageSummary;
import com.synergen.vobworkbench.model.EligibilityResult;
import com.synergen.vobworkbench.model.InsuranceOrder;
import com.synergen.vobworkbench.model.InsurancePolicy;
import com.synergen.vobworkbench.model.NetworkStatus;
import com.synergen.vobworkbench.model.Patient;
import com.synergen.vobworkbench.model.PlanType;
import com.synergen.vobworkbench.model.Priority;
import com.synergen.vobworkbench.model.Procedure;
import com.synergen.vobworkbench.model.Role;
import com.synergen.vobworkbench.model.User;
import com.synergen.vobworkbench.model.VobRequest;
import com.synergen.vobworkbench.model.VobStatus;
import com.synergen.vobworkbench.repository.PatientRepository;
import com.synergen.vobworkbench.repository.RefreshTokenRepository;
import com.synergen.vobworkbench.repository.UserRepository;
import com.synergen.vobworkbench.repository.VobRequestRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(TestDatabaseCleanupExtension.class)
class VobRequestAssignmentIntegrationTests {
    private static final Pattern JSON_STRING_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository users;

    @Autowired
    private PatientRepository patients;

    @Autowired
    private VobRequestRepository requests;

    @Autowired
    private RefreshTokenRepository refreshTokens;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String firstSpecialistUsername;
    private String secondSpecialistUsername;
    private Patient patient;

    @BeforeEach
    void setUp() {
        refreshTokens.deleteAll();

        Instant now = Instant.now();
        firstSpecialistUsername = "assignment_specialist_one_" + System.nanoTime();
        secondSpecialistUsername = "assignment_specialist_two_" + System.nanoTime();
        users.save(new User(null, firstSpecialistUsername, passwordEncoder.encode("password123"), firstSpecialistUsername,
                Role.SPECIALIST, true, now, now));
        users.save(new User(null, secondSpecialistUsername, passwordEncoder.encode("password123"), secondSpecialistUsername,
                Role.SPECIALIST, true, now, now));
        patient = patients.save(new Patient(null, "MRN-" + System.nanoTime(), "Assign", "Test",
                LocalDate.of(1988, 1, 1), "F", "555-0100", now, now));
    }

    @Test
    void queuedRequestsAreDispatchedToLeastLoadedOnlineSpecialist() throws Exception {
        String firstRequestId = createQueuedRequest();
        login(firstSpecialistUsername);

        assertAssignedTo(firstRequestId, firstSpecialistUsername);

        String secondRequestId = createQueuedRequest();
        login(secondSpecialistUsername);

        assertAssignedTo(secondRequestId, secondSpecialistUsername);
    }

    @Test
    void specialistApiAccessIsLimitedToAssignedRequests() throws Exception {
        String ownRequestId = createAssignedRequest(firstSpecialistUsername);
        String otherRequestId = createAssignedRequest(secondSpecialistUsername);
        String accessToken = field(login(firstSpecialistUsername).body(), "accessToken");

        HttpResponse<String> search = send("GET", "/api/vob-requests", null, accessToken);

        assertThat(search.statusCode()).isEqualTo(200);
        assertThat(search.body()).contains(ownRequestId);
        assertThat(search.body()).doesNotContain(otherRequestId);

        HttpResponse<String> ownRequest = send("GET", "/api/vob-requests/" + ownRequestId, null, accessToken);
        HttpResponse<String> otherRequest = send("GET", "/api/vob-requests/" + otherRequestId, null, accessToken);

        assertThat(ownRequest.statusCode()).isEqualTo(200);
        assertThat(otherRequest.statusCode()).isEqualTo(403);
        assertThat(otherRequest.body()).contains("VOB_REQUEST_NOT_ASSIGNED");
    }

    @Test
    void manualEligibilityMovesRequestToSpecialistReview() throws Exception {
        String requestId = createAssignedRequest(firstSpecialistUsername);
        String accessToken = field(login(firstSpecialistUsername).body(), "accessToken");

        send("GET", "/api/vob-requests/" + requestId, null, accessToken);
        Long version = requests.findById(requestId).orElseThrow().getVersion();

        HttpResponse<String> response = send("PATCH", "/api/vob-requests/" + requestId, """
                {
                  "eligibilityResult": {
                    "coverageActive": true,
                    "networkStatus": "IN_NETWORK",
                    "copay": 20,
                    "coinsurancePercent": 20,
                    "deductibleRemaining": 0,
                    "notes": "Manual eligibility review."
                  },
                  "version": %d
                }
                """.formatted(version), accessToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(requests.findById(requestId)).hasValueSatisfying(request ->
                assertThat(request.getStatus()).isEqualTo(VobStatus.SPECIALIST_REVIEW));
    }

    @Test
    void readyInProgressRequestCanBeVerified() throws Exception {
        String requestId = createReadyInProgressRequest(firstSpecialistUsername);
        String accessToken = field(login(firstSpecialistUsername).body(), "accessToken");

        HttpResponse<String> response = send("POST", "/api/vob-requests/" + requestId + "/verify", null, accessToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(requests.findById(requestId)).hasValueSatisfying(request -> {
            assertThat(request.getStatus()).isEqualTo(VobStatus.VERIFIED);
            assertThat(request.isLocked()).isTrue();
        });
    }

    private String createQueuedRequest() {
        Instant now = Instant.now();
        VobRequest request = new VobRequest();
        request.setPatientId(patient.getId());
        request.setDateOfService(LocalDate.now().plusDays(1));
        request.setPriority(Priority.ROUTINE);
        request.setStatus(VobStatus.PENDING);
        request.setAssignedTo(null);
        request.setCreatedBy("receptionist1");
        request.setInsurancePolicies(List.of(new InsurancePolicy("SynerCare Insurance", "MEM-100", "GRP-100",
                PlanType.PPO, InsuranceOrder.PRIMARY, LocalDate.now(), null, true)));
        request.setProcedures(List.of(new Procedure("IMG-001", "X-Ray", BigDecimal.valueOf(250), false)));
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        return requests.save(request).getId();
    }

    private String createAssignedRequest(String username) {
        Instant now = Instant.now();
        VobRequest request = new VobRequest();
        request.setPatientId(patient.getId());
        request.setDateOfService(LocalDate.now().plusDays(1));
        request.setPriority(Priority.ROUTINE);
        request.setStatus(VobStatus.PENDING);
        request.setAssignedTo(username);
        request.setCreatedBy("receptionist1");
        request.setInsurancePolicies(List.of(new InsurancePolicy("SynerCare Insurance", "MEM-100", "GRP-100",
                PlanType.PPO, InsuranceOrder.PRIMARY, LocalDate.now(), null, true)));
        request.setProcedures(List.of(new Procedure("IMG-001", "X-Ray", BigDecimal.valueOf(250), false)));
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        return requests.save(request).getId();
    }

    private String createReadyInProgressRequest(String username) {
        Instant now = Instant.now();
        VobRequest request = new VobRequest();
        request.setPatientId(patient.getId());
        request.setDateOfService(LocalDate.now().plusDays(1));
        request.setPriority(Priority.ROUTINE);
        request.setStatus(VobStatus.IN_PROGRESS);
        request.setAssignedTo(username);
        request.setCreatedBy("receptionist1");
        request.setInsurancePolicies(List.of(new InsurancePolicy("SynerCare Insurance", "MEM-100", "GRP-100",
                PlanType.PPO, InsuranceOrder.PRIMARY, LocalDate.now(), null, true)));
        request.setProcedures(List.of(new Procedure("IMG-001", "X-Ray", BigDecimal.valueOf(250), false)));
        request.setEligibilityResult(new EligibilityResult(true, NetworkStatus.IN_NETWORK, BigDecimal.valueOf(20), 20,
                BigDecimal.ZERO, "Manual eligibility review.", null, null, null, null, null, false, "MANUAL",
                username, now));
        request.setCoverageSummary(new CoverageSummary(BigDecimal.valueOf(250), BigDecimal.valueOf(180),
                BigDecimal.valueOf(70), BigDecimal.valueOf(250), BigDecimal.ZERO, BigDecimal.valueOf(20),
                BigDecimal.valueOf(50), BigDecimal.ZERO, List.of()));
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        return requests.save(request).getId();
    }

    private HttpResponse<String> login(String username) throws Exception {
        HttpResponse<String> response = send("POST", "/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"password123\"}", null);
        assertThat(response.statusCode()).isEqualTo(200);
        return response;
    }

    private void assertAssignedTo(String requestId, String username) {
        assertThat(requests.findById(requestId)).hasValueSatisfying(request -> {
            assertThat(request.getAssignedTo()).isEqualTo(username);
            assertThat(request.getStatus()).isEqualTo(VobStatus.PENDING);
        });
    }

    private HttpResponse<String> send(String method, String path, String body, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String field(String json, String fieldName) {
        Matcher matcher = Pattern.compile(JSON_STRING_FIELD.pattern().formatted(fieldName)).matcher(json);
        assertThat(matcher.find()).as("JSON field " + fieldName + " exists in " + json).isTrue();
        return matcher.group(1);
    }
}
