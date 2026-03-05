package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_refresh_tokens_token_jti", columnNames = "token_jti")
        },
        indexes = {
                @Index(name = "ix_refresh_tokens_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends AbstractCreatedEntity {

    public static final String CODE_REFRESH_TOKEN_USER_REQUIRED = "REFRESH_TOKEN_USER_REQUIRED";
    public static final String CODE_REFRESH_TOKEN_JTI_REQUIRED = "REFRESH_TOKEN_JTI_REQUIRED";
    public static final String CODE_REFRESH_TOKEN_EXPIRES_AT_REQUIRED = "REFRESH_TOKEN_EXPIRES_AT_REQUIRED";

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "token_jti", nullable = false, length = 36)
    private String tokenJti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RefreshToken(UUID userId, String tokenJti, Instant expiresAt) {
        super(UuidV7Generator.next());
        this.userId = requireUserId(userId);
        this.tokenJti = requireTokenJti(tokenJti);
        this.expiresAt = requireExpiresAt(expiresAt);
    }

    public static RefreshToken create(UUID userId, String tokenJti, Instant expiresAt) {
        return new RefreshToken(userId, tokenJti, expiresAt);
    }

    public static RefreshToken create(User user, String tokenJti, Instant expiresAt) {
        if (user == null) {
            throw new DomainException(CODE_REFRESH_TOKEN_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        RefreshToken refreshToken = new RefreshToken(user.getId(), tokenJti, expiresAt);
        refreshToken.user = user;
        return refreshToken;
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_REFRESH_TOKEN_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        return value;
    }

    private String requireTokenJti(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_REFRESH_TOKEN_JTI_REQUIRED, "토큰 JTI는 필수입니다");
        }
        return value;
    }

    private Instant requireExpiresAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_REFRESH_TOKEN_EXPIRES_AT_REQUIRED, "만료 시각은 필수입니다");
        }
        return value;
    }
}
