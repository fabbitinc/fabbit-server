package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshTokenRelationTest {

    @Test
    void refreshToken_엔티티_입력시_FK와_연관을_동기화한다() {
        User user = new User("user@example.com", "hashed", "User");
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        RefreshToken token = RefreshToken.create(user, "jti-123", expiresAt);

        assertEquals(user, token.getUser());
        assertEquals(user.getId(), token.getUserId());
        assertEquals("jti-123", token.getTokenJti());
        assertEquals(expiresAt, token.getExpiresAt());
    }

    @Test
    void refreshToken_사용자가_null이면_예외를_던진다() {
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        DomainException ex = assertThrows(DomainException.class, () -> RefreshToken.create(
                (User) null,
                "jti-123",
                expiresAt
        ));

        assertEquals(RefreshToken.CODE_REFRESH_TOKEN_USER_REQUIRED, ex.getDomainCode());
    }

    @Test
    void refreshToken_jti가_비어있으면_예외를_던진다() {
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        DomainException ex = assertThrows(DomainException.class, () -> RefreshToken.create(
                UUID.randomUUID(),
                "   ",
                expiresAt
        ));

        assertEquals(RefreshToken.CODE_REFRESH_TOKEN_JTI_REQUIRED, ex.getDomainCode());
    }
}
