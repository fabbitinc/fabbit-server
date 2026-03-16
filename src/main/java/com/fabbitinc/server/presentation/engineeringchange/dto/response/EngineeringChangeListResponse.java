package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "변경관리 목록 응답")
public record EngineeringChangeListResponse(
        long openCount,
        long closedCount,
        long total,
        int offset,
        int limit,
        List<EngineeringChangeSummaryResponse> items
) {
}
