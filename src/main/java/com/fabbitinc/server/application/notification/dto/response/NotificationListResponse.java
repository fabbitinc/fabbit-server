package com.fabbitinc.server.application.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record NotificationListResponse(
        List<NotificationResponse> items,
        UUID nextCursor,
        Map<String, NotificationUserSummaryResponse> users
) {
}
