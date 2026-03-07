package com.fabbitinc.server.application.notification.event;

import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import java.util.UUID;

public record NotificationCreatedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID userId
) {

    public static NotificationCreatedEvent create(UUID notificationId, UUID userId) {
        return new NotificationCreatedEvent(UuidV7Generator.next(), notificationId, userId);
    }
}
