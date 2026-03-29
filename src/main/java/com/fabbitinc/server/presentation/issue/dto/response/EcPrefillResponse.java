package com.fabbitinc.server.presentation.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "이슈 기반 EC 사전 입력 응답")
public record EcPrefillResponse(
        @Schema(description = "추천 제목 (이슈 제목)")
        String suggestedTitle,
        @Schema(description = "영향 항목 후보 목록")
        List<AffectedItemSuggestionResponse> affectedItems,
        @Schema(description = "추천 검토자 사용자 ID 목록")
        List<UUID> suggestedReviewerIds,
        @Schema(description = "영향 분석 요약")
        ImpactSummaryResponse impactSummary
) {
    @Schema(description = "영향 항목 후보")
    public record AffectedItemSuggestionResponse(
            @Schema(description = "부품 ID")
            UUID partId,
            @Schema(description = "품번", example = "PRT-001")
            String partNumber,
            @Schema(description = "리비전 ID")
            UUID revisionId,
            @Schema(description = "리비전 코드", example = "A")
            String revisionCode
    ) {
    }

    @Schema(description = "영향 분석 요약")
    public record ImpactSummaryResponse(
            @Schema(description = "영향받는 BOM 수", example = "5")
            int affectedBomCount,
            @Schema(description = "영향받는 프로젝트 수", example = "2")
            int affectedProjectCount,
            @Schema(description = "수정 필요 DRAFT 리비전 수", example = "3")
            int draftRevisionCount
    ) {
    }
}
