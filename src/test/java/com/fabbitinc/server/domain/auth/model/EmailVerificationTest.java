package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailVerificationTest {

    @Test
    void createPending_초기상태를_설정한다() {
        Instant expiresAt = Instant.now().plusSeconds(300);

        EmailVerification verification = EmailVerification.createPending(
                "user@example.com",
                "code_hash",
                expiresAt
        );

        assertEquals("user@example.com", verification.getEmail());
        assertEquals("code_hash", verification.getCodeHash());
        assertEquals(EmailVerificationStatus.PENDING, verification.getStatus());
        assertEquals(0, verification.getAttemptCount());
        assertEquals(expiresAt, verification.getExpiresAt());
    }

    @Test
    void ensureResendable_쿨다운이_남아있으면_예외를_던진다() {
        Instant now = Instant.now();
        EmailVerification verification = EmailVerification.createPending(
                "user@example.com",
                "code_hash",
                now.plusSeconds(300)
        );
        ReflectionTestUtils.setField(verification, "createdAt", now.minusSeconds(10));

        DomainException ex = assertThrows(
                DomainException.class,
                () -> verification.ensureResendable(now, 60)
        );

        assertEquals(EmailVerification.CODE_VERIFICATION_COOLDOWN, ex.getDomainCode());
    }

    @Test
    void ensureVerifiable_만료된_코드면_예외를_던진다() {
        Instant now = Instant.now();
        EmailVerification verification = EmailVerification.createPending(
                "user@example.com",
                "code_hash",
                now.minusSeconds(1)
        );

        DomainException ex = assertThrows(
                DomainException.class,
                () -> verification.ensureVerifiable(now, 5)
        );

        assertEquals(EmailVerification.CODE_VERIFICATION_EXPIRED, ex.getDomainCode());
    }

    @Test
    void registerFailedAttempt_최대시도에_도달하면_예외를_던진다() {
        EmailVerification verification = EmailVerification.createPending(
                "user@example.com",
                "code_hash",
                Instant.now().plusSeconds(300)
        );

        verification.registerFailedAttempt(2);

        DomainException ex = assertThrows(
                DomainException.class,
                () -> verification.registerFailedAttempt(2)
        );

        assertEquals(EmailVerification.CODE_VERIFICATION_MAX_ATTEMPTS, ex.getDomainCode());
        assertEquals(2, verification.getAttemptCount());
    }
}
