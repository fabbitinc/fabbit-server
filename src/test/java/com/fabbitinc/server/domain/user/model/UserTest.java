package com.fabbitinc.server.domain.user.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    @Test
    void user_create는_필수필드를_trim_정규화한다() {
        User user = User.create("  user@example.com  ", "  hashed  ", "  User Name  ");

        assertEquals("user@example.com", user.getEmail());
        assertEquals("hashed", user.getHashedPassword());
        assertEquals("User Name", user.getFullName());
        assertTrue(user.isActive());
    }

    @Test
    void user_create_이름이_blank면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                User.create("user@example.com", "hashed", "   ")
        );

        assertEquals(User.CODE_USER_FULL_NAME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void user_changeProfile은_전화번호_blank를_null로_정규화한다() {
        User user = User.create("user@example.com", "hashed", "User");

        user.changeProfile("  New Name  ", "   ");

        assertEquals("New Name", user.getFullName());
        assertNull(user.getPhone());
    }

    @Test
    void user_changeProfileImage는_blank면_예외를_던진다() {
        User user = User.create("user@example.com", "hashed", "User");

        DomainException ex = assertThrows(DomainException.class, () -> user.changeProfileImage("   "));

        assertEquals(User.CODE_USER_PROFILE_IMAGE_REQUIRED, ex.getDomainCode());
    }

    @Test
    void user_changeProfileImage가_너무_길면_예외를_던진다() {
        User user = User.create("user@example.com", "hashed", "User");

        DomainException ex = assertThrows(DomainException.class, () -> user.changeProfileImage("a".repeat(1001)));

        assertEquals(User.CODE_USER_PROFILE_IMAGE_TOO_LONG, ex.getDomainCode());
    }

    @Test
    void user_activate_deactivate는_상태전이를_강제한다() {
        User user = User.create("user@example.com", "hashed", "User");

        user.deactivate();
        assertFalse(user.isActive());

        user.activate();
        assertTrue(user.isActive());
    }

    @Test
    void user_deactivate_재호출이면_예외를_던진다() {
        User user = User.create("user@example.com", "hashed", "User");
        user.deactivate();

        DomainException ex = assertThrows(DomainException.class, user::deactivate);

        assertEquals(User.CODE_USER_ALREADY_INACTIVE, ex.getDomainCode());
    }
}
