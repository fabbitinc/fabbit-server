package com.fabbitinc.server.application.notification.event;

import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import java.util.UUID;

public record NotificationCreatedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID userId,
        UUID actorId,
        String actorFullName,
        String actorProfileImageFileKey,
        UUID sourceIssueId,
        int sourceNumber,
        String sourceTitle,
        String sourceIssueType,
        boolean comment
) {

    public static NotificationCreatedEvent create(
            UUID notificationId,
            UUID userId,
            UUID actorId,
            String actorFullName,
            String actorProfileImageFileKey,
            UUID sourceIssueId,
            int sourceNumber,
            String sourceTitle,
            String sourceIssueType,
            boolean comment
    ) {
        return new NotificationCreatedEvent(
                UuidV7Generator.next(),
                notificationId,
                userId,
                actorId,
                actorFullName,
                actorProfileImageFileKey,
                sourceIssueId,
                sourceNumber,
                sourceTitle,
                sourceIssueType,
                comment
        );
    }
}
