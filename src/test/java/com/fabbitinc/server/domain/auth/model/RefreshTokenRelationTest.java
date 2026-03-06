package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenRelationTest {

    @Test
    void refreshToken_create_입력값을_보관한다() {
        UUID userId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-03-10T00:00:00Z");

        RefreshToken token = RefreshToken.create(userId, "jti-123", expiresAt);

        assertEquals(userId, token.getUserId());
        assertEquals("jti-123", token.getTokenJti());
        assertEquals(expiresAt, token.getExpiresAt());
    }

    @Test
    void refreshToken_사용자가_null이면_예외를_던진다() {
        Instant expiresAt = Instant.parse("2026-03-10T00:00:00Z");

        DomainException ex = assertThrows(DomainException.class, () -> RefreshToken.create(
                null,
                "jti-123",
                expiresAt
        ));

        assertEquals(RefreshToken.CODE_REFRESH_TOKEN_USER_REQUIRED, ex.getDomainCode());
    }

    @Test
    void refreshToken_jti가_비어있으면_예외를_던진다() {
        Instant expiresAt = Instant.parse("2026-03-10T00:00:00Z");

        DomainException ex = assertThrows(DomainException.class, () -> RefreshToken.create(
                UUID.randomUUID(),
                "   ",
                expiresAt
        ));

        assertEquals(RefreshToken.CODE_REFRESH_TOKEN_JTI_REQUIRED, ex.getDomainCode());
    }

    @Test
    void refreshToken_rotate는_같은사용자로_새토큰을_발급한다() {
        RefreshToken token = RefreshToken.create(
                UUID.randomUUID(),
                "jti-123",
                Instant.parse("2026-03-10T00:00:00Z")
        );

        RefreshToken rotated = token.rotate("jti-456", Instant.parse("2026-03-20T00:00:00Z"));

        assertEquals(token.getUserId(), rotated.getUserId());
        assertEquals("jti-456", rotated.getTokenJti());
        assertEquals(Instant.parse("2026-03-20T00:00:00Z"), rotated.getExpiresAt());
    }

    @Test
    void refreshToken_validateOwnedBy는_다른사용자면_예외를_던진다() {
        RefreshToken token = RefreshToken.create(
                UUID.randomUUID(),
                "jti-123",
                Instant.parse("2026-03-10T00:00:00Z")
        );

        DomainException ex = assertThrows(DomainException.class, () -> token.validateOwnedBy(UUID.randomUUID()));

        assertEquals(RefreshToken.CODE_REFRESH_TOKEN_INVALID_USER, ex.getDomainCode());
    }

    @Test
    void refreshToken_validateUsableAt은_만료토큰이면_예외를_던진다() {
        RefreshToken token = RefreshToken.create(
                UUID.randomUUID(),
                "jti-123",
                Instant.parse("2026-03-10T00:00:00Z")
        );

        DomainException ex = assertThrows(DomainException.class, () -> token.validateUsableAt(
                Instant.parse("2026-03-10T00:00:00Z")
        ));

        assertEquals(RefreshToken.CODE_REFRESH_TOKEN_EXPIRED, ex.getDomainCode());
    }
}
