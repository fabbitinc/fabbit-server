package com.fabbitinc.server.application.mapping.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "스킵된 시트 정보")
public record SkippedSheetResponse(
        @Schema(description = "시트명")
        String sheetName,
        @Schema(description = "스킵 사유")
        String reason
) {
}
