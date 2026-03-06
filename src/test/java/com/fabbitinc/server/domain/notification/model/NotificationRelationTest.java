package com.fabbitinc.server.domain.notification.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationRelationTest {

    @Test
    void notification_생성시_userId_actorId와_payload를_보관한다() {
        UUID userId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        Notification notification = Notification.create(userId, NotificationType.MENTION, actorId, "{\"k\":\"v\"}");

        assertEquals(userId, notification.getUserId());
        assertEquals(actorId, notification.getActorId());
        assertEquals(NotificationType.MENTION, notification.getType());
        assertEquals("{\"k\":\"v\"}", notification.getPayload());
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
        DomainException ex = assertThrows(DomainException.class, () ->
                Notification.create(null, NotificationType.MENTION, UUID.randomUUID(), "{}")
        );

        assertEquals(Notification.CODE_NOTIFICATION_USER_REQUIRED, ex.getDomainCode());
    }

    @Test
    void notification_행위자가_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                Notification.create(UUID.randomUUID(), NotificationType.MENTION, null, "{}")
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

    @Test
    void notification_markRead는_이미_읽은알림이면_noop이다() {
        Notification notification = Notification.create(
                UUID.randomUUID(),
                NotificationType.MENTION,
                UUID.randomUUID(),
                "{}"
        );
        Instant firstReadAt = Instant.parse("2026-03-01T00:00:00Z");
        Instant secondReadAt = Instant.parse("2026-03-02T00:00:00Z");

        notification.markRead(firstReadAt);
        notification.markRead(secondReadAt);

        assertEquals(firstReadAt, notification.getReadAt());
    }
}
