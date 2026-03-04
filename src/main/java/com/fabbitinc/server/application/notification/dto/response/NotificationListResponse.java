package com.fabbitinc.server.application.notification.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationListResponse(
        List<NotificationResponse> items,
        UUID nextCursor,
        Map<String, NotificationUserSummaryResponse> users
) {
}
