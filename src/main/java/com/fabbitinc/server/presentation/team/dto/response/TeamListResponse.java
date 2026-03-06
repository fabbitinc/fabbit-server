package com.fabbitinc.server.presentation.team.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeamListResponse(
        List<TeamSummaryItemResponse> items
) {
    public record TeamSummaryItemResponse(
            UUID id,
            String name,
            String description,
            int memberCount,
            UUID createdBy,
            Instant createdAt
    ) {
    }
}
