package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Duration;
import java.time.Instant;

@Getter
@Entity
@Table(
        name = "email_verifications",
        schema = "public",
        indexes = {
                @Index(name = "ix_email_verifications_email", columnList = "email")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends AbstractCreatedEntity {

    public static final String CODE_VERIFICATION_COOLDOWN = "AUTH_VERIFICATION_COOLDOWN";
    public static final String CODE_VERIFICATION_EXPIRED = "AUTH_VERIFICATION_EXPIRED";
    public static final String CODE_VERIFICATION_MAX_ATTEMPTS = "AUTH_VERIFICATION_MAX_ATTEMPTS";

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailVerificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private EmailVerification(String email, String codeHash, Instant expiresAt) {
        super(UuidV7Generator.next());
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.status = EmailVerificationStatus.PENDING;
        this.attemptCount = 0;
    }

    public static EmailVerification createPending(String email, String codeHash, Instant expiresAt) {
        return new EmailVerification(email, codeHash, expiresAt);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isMaxAttempts(int maxAttempts) {
        return attemptCount >= maxAttempts;
    }

    public void incrementAttempt() {
        this.attemptCount += 1;
    }

    public void ensureResendable(Instant now, long cooldownSeconds) {
        long elapsed = Duration.between(getCreatedAt(), now).toSeconds();
        if (elapsed < cooldownSeconds) {
            throw new DomainException(CODE_VERIFICATION_COOLDOWN, "잠시 후 다시 시도해 주세요");
        }
    }

    public void ensureVerifiable(Instant now, int maxAttempts) {
        if (isExpired(now)) {
            throw new DomainException(CODE_VERIFICATION_EXPIRED, "인증코드가 만료되었습니다. 재발송해 주세요");
        }
        if (isMaxAttempts(maxAttempts)) {
            throw new DomainException(
                    CODE_VERIFICATION_MAX_ATTEMPTS,
                    "인증 시도 횟수를 초과했습니다. 인증코드를 재발송해 주세요"
            );
        }
    }

    public void registerFailedAttempt(int maxAttempts) {
        incrementAttempt();
        if (isMaxAttempts(maxAttempts)) {
            throw new DomainException(
                    CODE_VERIFICATION_MAX_ATTEMPTS,
                    "인증 시도 횟수를 초과했습니다. 인증코드를 재발송해 주세요"
            );
        }
    }

    public void verify(String verificationTokenHash) {
        this.status = EmailVerificationStatus.VERIFIED;
        this.verificationTokenHash = verificationTokenHash;
    }

    public void use() {
        this.status = EmailVerificationStatus.USED;
    }
}
