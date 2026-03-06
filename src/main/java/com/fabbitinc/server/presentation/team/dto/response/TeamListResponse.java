package com.fabbitinc.server.presentation.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "응답 DTO")
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
