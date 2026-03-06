package com.fabbitinc.server.application.part.usecase.result;

import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;

import java.util.UUID;

public record RegisterPartDrawingResult(
        UUID drawingId,
        String drawingNumber,
        String name,
        DrawingConversionStatus conversionStatus
) {
}
