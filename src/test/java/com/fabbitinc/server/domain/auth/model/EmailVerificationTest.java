package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailVerificationTest {

    @Test
    void createPending_초기상태를_설정한다() {
        Instant expiresAt = Instant.now().plusSeconds(300);

        EmailVerification verification = EmailVerification.createPending(
                " User@Example.com ",
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
    void createPending_이메일이_blank면_예외를_던진다() {
        DomainException ex = assertThrows(
                DomainException.class,
                () -> EmailVerification.createPending("   ", "code_hash", Instant.now().plusSeconds(300))
        );

        assertEquals(EmailVerification.CODE_VERIFICATION_EMAIL_REQUIRED, ex.getDomainCode());
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

    @Test
    void verify_이미_VERIFIED면_예외를_던지고_기존상태를_유지한다() {
        EmailVerification verification = EmailVerification.createPending(
                "user@example.com",
                "code_hash",
                Instant.now().plusSeconds(300)
        );
        verification.verify("token_hash");

        DomainException ex = assertThrows(
                DomainException.class,
                () -> verification.verify("new_token_hash")
        );

        assertEquals(EmailVerification.CODE_VERIFICATION_INVALID_STATE, ex.getDomainCode());
        assertEquals(EmailVerificationStatus.VERIFIED, verification.getStatus());
        assertEquals("token_hash", verification.getVerificationTokenHash());
    }

    @Test
    void use_PENDING상태면_예외를_던지고_상태를_유지한다() {
        EmailVerification verification = EmailVerification.createPending(
                "user@example.com",
                "code_hash",
                Instant.now().plusSeconds(300)
        );

        DomainException ex = assertThrows(DomainException.class, verification::use);

        assertEquals(EmailVerification.CODE_VERIFICATION_INVALID_STATE, ex.getDomainCode());
        assertEquals(EmailVerificationStatus.PENDING, verification.getStatus());
        assertNull(verification.getVerificationTokenHash());
    }

    @Test
    void isExpired_now가_null이면_예외를_던진다() {
        EmailVerification verification = EmailVerification.createPending(
                "user@example.com",
                "code_hash",
                Instant.now().plusSeconds(300)
        );

        DomainException ex = assertThrows(DomainException.class, () -> verification.isExpired(null));

        assertEquals(EmailVerification.CODE_VERIFICATION_TIME_REQUIRED, ex.getDomainCode());
    }
}
