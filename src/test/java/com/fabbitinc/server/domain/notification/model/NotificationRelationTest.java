package com.fabbitinc.server.domain.notification.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationRelationTest {

    @Test
    void notification_엔티티_입력시_user_actor_FK와_연관을_동기화한다() {
        User user = new User("receiver@example.com", "hashed", "Receiver");
        User actor = new User("actor@example.com", "hashed", "Actor");

        Notification notification = Notification.create(user, NotificationType.MENTION, actor, "{\"k\":\"v\"}");

        assertEquals(user, notification.getUser());
        assertEquals(user.getId(), notification.getUserId());
        assertEquals(actor, notification.getActor());
        assertEquals(actor.getId(), notification.getActorId());
        assertEquals(NotificationType.MENTION, notification.getType());
    }

    @Test
    void notification_payload가_null이면_빈_json으로_정규화한다() {
        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.MENTION,
                UUID.randomUUID(),
                null
        );

        assertEquals("{}", notification.getPayload());
    }

    @Test
    void notification_payload는_trim_정규화한다() {
        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.MENTION,
                UUID.randomUUID(),
                "  {\"k\":\"v\"}  "
        );

        assertEquals("{\"k\":\"v\"}", notification.getPayload());
    }

    @Test
    void notification_수신자가_null이면_예외를_던진다() {
        User actor = new User("actor@example.com", "hashed", "Actor");

        DomainException ex = assertThrows(DomainException.class, () ->
                Notification.create(null, NotificationType.MENTION, actor, "{}")
        );

        assertEquals(Notification.CODE_NOTIFICATION_USER_REQUIRED, ex.getDomainCode());
    }

    @Test
    void notification_행위자가_null이면_예외를_던진다() {
        User user = new User("receiver@example.com", "hashed", "Receiver");

        DomainException ex = assertThrows(DomainException.class, () ->
                Notification.create(user, NotificationType.MENTION, null, "{}")
        );

        assertEquals(Notification.CODE_NOTIFICATION_ACTOR_REQUIRED, ex.getDomainCode());
    }

    @Test
    void notification_markRead_readAt가_null이면_예외를_던지고_값을_유지한다() {
        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.MENTION,
                UUID.randomUUID(),
                "{}"
        );

        DomainException ex = assertThrows(DomainException.class, () -> notification.markRead(null));

        assertEquals(Notification.CODE_NOTIFICATION_READ_AT_REQUIRED, ex.getDomainCode());
        assertNull(notification.getReadAt());
    }

    @Test
    void notification_markRead_읽음시각을_설정한다() {
        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.MENTION,
                UUID.randomUUID(),
                "{}"
        );
        Instant readAt = Instant.now();

        notification.markRead(readAt);

        assertEquals(readAt, notification.getReadAt());
    }
}
