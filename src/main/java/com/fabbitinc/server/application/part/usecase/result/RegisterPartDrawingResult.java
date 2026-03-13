package com.fabbitinc.server.application.part.usecase.result;

import java.util.UUID;

public record RegisterPartDrawingResult(
        UUID drawingId,
        String drawingNumber,
        String name
) {
}
