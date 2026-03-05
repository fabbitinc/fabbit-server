package com.fabbitinc.server.domain.notification.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
