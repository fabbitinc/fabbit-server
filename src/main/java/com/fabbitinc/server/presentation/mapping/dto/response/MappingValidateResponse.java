package com.fabbitinc.server.presentation.mapping.dto.response;

import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "매핑 검증 응답")
public record MappingValidateResponse(
        @Schema(description = "정규화된 매핑")
        MappingResultDto normalizedMapping,
        @Schema(description = "오류 목록")
        List<ValidationIssueResponse> errors,
        @Schema(description = "경고 목록")
        List<ValidationIssueResponse> warnings,
        @Schema(description = "영향 요약")
        MappingImpactSummaryResponse impactSummary
) {
}
