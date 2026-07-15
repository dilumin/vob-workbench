package com.synergen.vobworkbench.repository;

import com.synergen.vobworkbench.model.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByRevokedFalseAndExpiresAtAfter(Instant now);
}
