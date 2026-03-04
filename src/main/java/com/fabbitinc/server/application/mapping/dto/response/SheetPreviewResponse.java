package com.fabbitinc.server.application.mapping.dto.response;

import tools.jackson.databind.JsonNode;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "시트별 매핑 미리보기")
public record SheetPreviewResponse(
        @Schema(description = "시트명")
        String sheetName,
        @Schema(description = "헤더 목록")
        List<String> headers,
        @Schema(description = "샘플 행 목록")
        List<JsonNode> sampleRows,
        @Schema(description = "시트 매핑 결과")
        MappingResultDto mapping
) {
}
