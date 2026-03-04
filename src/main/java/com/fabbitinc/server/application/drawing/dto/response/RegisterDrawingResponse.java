package com.fabbitinc.server.application.drawing.dto.response;

import java.util.UUID;

public record RegisterDrawingResponse(
        UUID drawingId,
        String drawingNumber,
        String name,
        String conversionStatus
) {
}
