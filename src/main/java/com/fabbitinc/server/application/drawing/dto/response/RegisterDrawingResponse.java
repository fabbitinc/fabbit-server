package com.fabbitinc.server.application.drawing.dto.response;

import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;

import java.util.UUID;

public record RegisterDrawingResponse(
        UUID drawingId,
        String drawingNumber,
        String name,
        DrawingConversionStatus conversionStatus
) {
}
