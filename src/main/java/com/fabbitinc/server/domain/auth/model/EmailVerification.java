package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
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

    public void verify(String verificationTokenHash) {
        this.status = EmailVerificationStatus.VERIFIED;
        this.verificationTokenHash = verificationTokenHash;
    }

    public void use() {
        this.status = EmailVerificationStatus.USED;
    }
}
