package com.fabbitinc.server.domain.auth.repository;

import com.fabbitinc.server.domain.auth.model.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenJti(String tokenJti);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
}
