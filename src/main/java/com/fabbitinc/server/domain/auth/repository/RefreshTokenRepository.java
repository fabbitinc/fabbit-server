package com.fabbitinc.server.domain.auth.repository;

import com.fabbitinc.server.domain.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenJti(String tokenJti);

    void deleteByTokenJti(String tokenJti);

    void deleteByUserId(UUID userId);
}
