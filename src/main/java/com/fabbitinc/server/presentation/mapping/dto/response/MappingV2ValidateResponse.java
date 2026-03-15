package com.fabbitinc.server.presentation.mapping.dto.response;

import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "V2 매핑 검증 응답")
public record MappingV2ValidateResponse(
        @Schema(description = "정규화된 V2 매핑")
        MappingV2ResultDto normalizedMapping,
        @Schema(description = "오류 목록")
        List<ValidationIssueResponse> errors,
        @Schema(description = "경고 목록")
        List<ValidationIssueResponse> warnings,
        @Schema(description = "영향 요약")
        MappingImpactSummaryResponse impactSummary
) {
}
