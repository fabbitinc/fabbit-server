package com.fabbitinc.server.presentation.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Part 통계")
public record PartStatsResponse(
        @Schema(description = "전체 Part 수", example = "1280")
        int total,
        @Schema(description = "이번 주 추가된 Part 수", example = "42")
        int addedThisWeek
) {
}
