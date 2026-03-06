package com.fabbitinc.server.application.notification.query.condition;

import java.util.UUID;

public record NotificationListCondition(
        UUID cursor,
        int limit,
        boolean unreadOnly
) {
}
