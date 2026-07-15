package com.synergen.vobworkbench.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refreshTokens")
public class RefreshToken {
    @Id
    private String id;

    @Indexed
    private String username;

    @Indexed(unique = true)
    private String tokenHash;

    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;
    private Instant revokedAt;
}
