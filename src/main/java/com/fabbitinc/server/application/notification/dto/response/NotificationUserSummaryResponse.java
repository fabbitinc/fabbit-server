package com.fabbitinc.server.application.notification.dto.response;

import java.util.UUID;

public record NotificationUserSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
