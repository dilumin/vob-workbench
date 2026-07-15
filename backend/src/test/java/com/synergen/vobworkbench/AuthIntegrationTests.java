package com.synergen.vobworkbench;

import static org.assertj.core.api.Assertions.assertThat;

import com.synergen.vobworkbench.model.AuditEvent;
import com.synergen.vobworkbench.model.Role;
import com.synergen.vobworkbench.model.User;
import com.synergen.vobworkbench.repository.AuditEventRepository;
import com.synergen.vobworkbench.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
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
class AuthIntegrationTests {
    private static final Pattern JSON_STRING_FIELD = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuditEventRepository auditEvents;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String adminUsername;
    private String viewerUsername;

    @BeforeEach
    void setUp() {
        adminUsername = "auth_admin_" + System.nanoTime();
        viewerUsername = "auth_viewer_" + System.nanoTime();
        createUser(adminUsername, Role.SUPERVISOR_ADMIN);
        createUser(viewerUsername, Role.VIEWER);
    }

    @Test
    void loginSuccessReturnsAccessAndRefreshTokens() throws Exception {
        HttpResponse<String> response = login(adminUsername, "password123");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(field(response.body(), "accessToken")).isNotBlank();
        assertThat(field(response.body(), "refreshToken")).isNotBlank();
        assertThat(field(response.body(), "tokenType")).isEqualTo("Bearer");
        assertThat(response.body()).contains("\"expiresIn\":900");
        assertThat(response.body()).contains("SUPERVISOR_ADMIN");
        assertThat(hasAuditEvent("USER_LOGGED_IN", adminUsername)).isTrue();
    }

    @Test
    void loginWrongPasswordReturnsUnauthorized() throws Exception {
        HttpResponse<String> response = login(adminUsername, "wrong");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("AUTHENTICATION_FAILED");
        assertThat(hasAuditEvent("USER_LOGIN_FAILED", adminUsername)).isTrue();
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        HttpResponse<String> response = send("GET", "/api/auth/me", null, null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void protectedEndpointAcceptsValidAccessToken() throws Exception {
        String accessToken = field(login(adminUsername, "password123").body(), "accessToken");

        HttpResponse<String> response = send("GET", "/api/auth/me", null, accessToken);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(adminUsername);
    }

    @Test
    void invalidAccessTokenIsRejected() throws Exception {
        HttpResponse<String> response = send("GET", "/api/auth/me", null, "not-a-jwt");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void refreshTokenRotatesTokensAndRejectsOldRefreshToken() throws Exception {
        String loginBody = login(adminUsername, "password123").body();
        String oldRefreshToken = field(loginBody, "refreshToken");

        HttpResponse<String> refresh = send("POST", "/api/auth/refresh",
                "{\"refreshToken\":\"" + oldRefreshToken + "\"}", null);

        assertThat(refresh.statusCode()).isEqualTo(200);
        assertThat(field(refresh.body(), "accessToken")).isNotBlank();
        assertThat(field(refresh.body(), "refreshToken")).isNotEqualTo(oldRefreshToken);

        HttpResponse<String> retry = send("POST", "/api/auth/refresh",
                "{\"refreshToken\":\"" + oldRefreshToken + "\"}", null);
        assertThat(retry.statusCode()).isEqualTo(401);
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String loginBody = login(adminUsername, "password123").body();
        String accessToken = field(loginBody, "accessToken");
        String refreshToken = field(loginBody, "refreshToken");

        HttpResponse<String> logout = send("POST", "/api/auth/logout",
                "{\"refreshToken\":\"" + refreshToken + "\"}", accessToken);

        assertThat(logout.statusCode()).isEqualTo(200);

        HttpResponse<String> refreshAfterLogout = send("POST", "/api/auth/refresh",
                "{\"refreshToken\":\"" + refreshToken + "\"}", null);
        assertThat(refreshAfterLogout.statusCode()).isEqualTo(401);
    }

    @Test
    void roleBasedAccessRejectsViewerFromAdminEndpoint() throws Exception {
        String viewerToken = field(login(viewerUsername, "password123").body(), "accessToken");

        HttpResponse<String> response = send("GET", "/api/admin/users", null, viewerToken);

        assertThat(response.statusCode()).isEqualTo(403);
    }

    @Test
    void adminUserCreationIsAuditedWithoutSensitiveValues() throws Exception {
        String accessToken = field(login(adminUsername, "password123").body(), "accessToken");
        String newUsername = "registered_user_" + System.nanoTime();

        HttpResponse<String> response = send("POST", "/api/admin/users", """
                {
                  "username": "%s",
                  "password": "secret-password",
                  "fullName": "Sensitive Name",
                  "role": "VIEWER"
                }
                """.formatted(newUsername), accessToken);

        assertThat(response.statusCode()).isEqualTo(201);
        AuditEvent event = auditEvents.findAll().stream()
                .filter(candidate -> "USER_REGISTERED".equals(candidate.getAction()))
                .filter(candidate -> newUsername.equals(candidate.getMetadata().get("targetUsername")))
                .findFirst()
                .orElseThrow();
        assertThat(event.getActorUsername()).isEqualTo(adminUsername);
        assertThat(event.getMetadata()).containsEntry("targetRole", "VIEWER");
        assertThat(event.toString()).doesNotContain("secret-password", "Sensitive Name");
    }

    private HttpResponse<String> login(String username, String password) throws Exception {
        return send("POST", "/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", null);
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

    private boolean hasAuditEvent(String action, String actorUsername) {
        return auditEvents.findAll().stream()
                .anyMatch(event -> action.equals(event.getAction()) && actorUsername.equals(event.getActorUsername()));
    }

    private void createUser(String username, Role role) {
        Instant now = Instant.now();
        users.save(new User(null, username, passwordEncoder.encode("password123"), username, role, true, now, now));
    }
}
