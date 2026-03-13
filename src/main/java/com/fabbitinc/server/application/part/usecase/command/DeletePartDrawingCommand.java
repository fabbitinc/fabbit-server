package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record DeletePartDrawingCommand(
        String partNumber,
        String revisionCode,
        UUID drawingId
) {
}
