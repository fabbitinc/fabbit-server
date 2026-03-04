package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_refresh_tokens_token_jti", columnNames = "token_jti")
        },
        indexes = {
                @Index(name = "ix_refresh_tokens_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends AbstractCreatedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_jti", nullable = false, length = 36)
    private String tokenJti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RefreshToken(UUID userId, String tokenJti, Instant expiresAt) {
        super(UuidV7Generator.next());
        this.userId = userId;
        this.tokenJti = tokenJti;
        this.expiresAt = expiresAt;
    }
}
