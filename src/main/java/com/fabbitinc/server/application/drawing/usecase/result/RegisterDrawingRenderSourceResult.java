package com.fabbitinc.server.application.drawing.usecase.result;

import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import java.util.UUID;

public record RegisterDrawingRenderSourceResult(
        UUID drawingId,
        DrawingConversionStatus conversionStatus
) {
}
