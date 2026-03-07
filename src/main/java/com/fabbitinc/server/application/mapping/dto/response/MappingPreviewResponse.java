package com.fabbitinc.server.application.mapping.dto.response;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import tools.jackson.databind.JsonNode;

@Schema(description = "매핑 미리보기 응답")
public record MappingPreviewResponse(
        @Schema(description = "대표 헤더 목록")
        List<String> headers,
        @Schema(description = "대표 샘플 행 목록")
        List<JsonNode> sampleRows,
        @Schema(description = "대표 매핑 결과")
        MappingResultDto mapping,
        @Schema(description = "시트별 미리보기 목록")
        List<SheetPreviewResponse> sheets,
        @Schema(description = "스킵된 시트 목록")
        List<SkippedSheetResponse> skippedSheets
) {
}
