package com.fabbitinc.server.application.notification.usecase.command;

import java.util.UUID;

public record PushNotificationStreamCommand(
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
}
