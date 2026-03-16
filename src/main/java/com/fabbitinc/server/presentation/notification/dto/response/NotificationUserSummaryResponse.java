package com.fabbitinc.server.presentation.notification.dto.response;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record NotificationUserSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
