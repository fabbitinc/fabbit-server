package com.fabbitinc.server.presentation.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "응답 DTO")
public record UnreadCountResponse(
        int count
) {
}
