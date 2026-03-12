package com.fabbitinc.server.application.drawing.usecase.command;

import java.util.UUID;

public record RegisterDrawingRenderSourceCommand(
        UUID drawingId,
        UUID fileId
) {
}
