package com.fabbitinc.server.application.activity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record ActivityListResponse(
        List<ActivityResponse> items,
        UUID nextCursor,
        Map<String, UserSummaryResponse> users
) {
}
