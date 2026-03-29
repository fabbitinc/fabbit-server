package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "부품 영향 분석 응답 DTO")
public record PartImpactAnalysisResponse(
        @Schema(description = "영향받는 BOM 항목 목록")
        List<AffectedBomItemResponse> bomItems,
        @Schema(description = "영향받는 프로젝트 목록")
        List<AffectedProjectResponse> projects,
        @Schema(description = "영향 분석 요약")
        SummaryResponse summary
) {

    @Schema(description = "영향받는 BOM 항목")
    public record AffectedBomItemResponse(
            @Schema(description = "상위 부품 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID parentPartId,
            @Schema(description = "상위 부품 품번", example = "PRT-001")
            String parentPartNumber,
            @Schema(description = "상위 부품 품명", example = "프레임 어셈블리")
            String parentPartName,
            @Schema(description = "상위 부품 리비전 코드", example = "A")
            String parentRevisionCode,
            @Schema(description = "BOM 트리 레벨 (1이 직접 상위)", example = "1")
            int level
    ) {
    }

    @Schema(description = "영향받는 프로젝트")
    public record AffectedProjectResponse(
            @Schema(description = "프로젝트 ID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID projectId,
            @Schema(description = "프로젝트 이름", example = "자동차 프레임 프로젝트")
            String projectName
    ) {
    }

    @Schema(description = "영향 분석 요약")
    public record SummaryResponse(
            @Schema(description = "영향받는 BOM 항목 수", example = "5")
            int affectedBomCount,
            @Schema(description = "영향받는 프로젝트 수", example = "2")
            int affectedProjectCount,
            @Schema(description = "DRAFT 상태 리비전 수", example = "1")
            int draftRevisionCount,
            @Schema(description = "추천 리뷰어 사용자 ID 목록")
            List<UUID> suggestedReviewerIds,
            @Schema(description = "결과 절삭 여부 (200건 초과 시 true)", example = "false")
            boolean truncated,
            @Schema(description = "전체 영향 항목 수", example = "5")
            int totalCount
    ) {
    }
}
