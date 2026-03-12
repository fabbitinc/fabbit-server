package com.fabbitinc.server.application.drawing.dto.response;

import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "도면 render source 등록 응답 DTO")
public record RegisterDrawingRenderSourceResponse(
        @Schema(description = "도면 ID")
        UUID drawingId,
        @Schema(description = "도면 변환 상태", example = "PENDING")
        DrawingConversionStatus conversionStatus
) {
}
