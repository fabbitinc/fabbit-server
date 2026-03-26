package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "변경관리 목록 응답")
public record EngineeringChangeListResponse(
        @Schema(description = "열림(DRAFT) 건수") long openCount,
        @Schema(description = "진행중(REVIEW_PENDING, APPROVAL_PENDING, RELEASE_PENDING) 건수") long progressCount,
        @Schema(description = "완료(RELEASED, CANCELED) 건수") long doneCount,
        long total,
        int offset,
        int limit,
        List<EngineeringChangeSummaryResponse> items
) {
}
