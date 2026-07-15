package com.synergen.vobworkbench.service;

import com.synergen.vobworkbench.exception.BusinessException;
import com.synergen.vobworkbench.model.RefreshToken;
import com.synergen.vobworkbench.model.User;
import com.synergen.vobworkbench.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokens;
    private final SecureRandom secureRandom = new SecureRandom();
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokens,
            @Value("${security.refresh-token.expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.refreshTokens = refreshTokens;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String createFor(User user) {
        String rawToken = generateRawToken();
        Instant now = Instant.now();
        RefreshToken token = new RefreshToken(
                null,
                user.getUsername(),
                hash(rawToken),
                now.plusMillis(refreshTokenExpirationMs),
                false,
                now,
                null
        );
        refreshTokens.save(token);
        return rawToken;
    }

    public RefreshToken validate(String rawToken) {
        RefreshToken token = refreshTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> invalidRefreshToken());

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw invalidRefreshToken();
        }
        return token;
    }

    public void revoke(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(this::revoke);
    }

    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        refreshTokens.save(token);
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash refresh token", ex);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid, expired, or revoked.");
    }
}
