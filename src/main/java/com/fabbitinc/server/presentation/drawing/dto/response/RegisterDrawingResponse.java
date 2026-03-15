package com.fabbitinc.server.presentation.drawing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record RegisterDrawingResponse(
        UUID drawingId,
        String drawingNumber,
        String name
) {
}
