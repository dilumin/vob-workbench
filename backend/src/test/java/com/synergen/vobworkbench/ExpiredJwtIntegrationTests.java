package com.synergen.vobworkbench;

import static org.assertj.core.api.Assertions.assertThat;

import com.synergen.vobworkbench.model.Role;
import com.synergen.vobworkbench.model.User;
import com.synergen.vobworkbench.repository.UserRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "security.jwt.access-token-expiration-ms=1",
                "security.refresh-token.expiration-ms=1"
        }
)
@ActiveProfiles("test")
@ExtendWith(TestDatabaseCleanupExtension.class)
class ExpiredJwtIntegrationTests {
    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern REFRESH_TOKEN = Pattern.compile("\"refreshToken\"\\s*:\\s*\"([^\"]+)\"");

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void expiredAccessTokenIsRejectedFromProtectedEndpoint() throws Exception {
        String username = "expired_user_" + System.nanoTime();
        createUser(username);

        HttpResponse<String> login = send("POST", "/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"password123\"}", null);
        String accessToken = accessToken(login.body());

        Thread.sleep(25);

        HttpResponse<String> response = send("GET", "/api/auth/me", null, accessToken);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void expiredRefreshTokenIsRejected() throws Exception {
        String username = "expired_refresh_user_" + System.nanoTime();
        createUser(username);

        HttpResponse<String> login = send("POST", "/api/auth/login",
                "{\"username\":\"" + username + "\",\"password\":\"password123\"}", null);
        String refreshToken = token(login.body(), REFRESH_TOKEN);

        Thread.sleep(25);

        HttpResponse<String> response = send("POST", "/api/auth/refresh",
                "{\"refreshToken\":\"" + refreshToken + "\"}", null);

        assertThat(response.statusCode()).isEqualTo(401);
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

    private String accessToken(String body) {
        return token(body, ACCESS_TOKEN);
    }

    private String token(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        assertThat(matcher.find()).as(body).isTrue();
        return matcher.group(1);
    }

    private void createUser(String username) {
        Instant now = Instant.now();
        users.save(new User(null, username, passwordEncoder.encode("password123"), username, Role.SUPERVISOR_ADMIN, true, now, now));
    }
}
