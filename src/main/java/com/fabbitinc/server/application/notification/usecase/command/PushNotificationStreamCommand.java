package com.fabbitinc.server.application.notification.usecase.command;

import java.util.UUID;

public record PushNotificationStreamCommand(
        UUID notificationId,
        UUID userId
) {
}
