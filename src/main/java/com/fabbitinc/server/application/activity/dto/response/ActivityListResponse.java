package com.fabbitinc.server.application.activity.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ActivityListResponse(
        List<ActivityResponse> items,
        UUID nextCursor,
        Map<String, UserSummaryResponse> users
) {
}
