package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record RegisterPartDrawingCommand(
        UUID partId,
        UUID revisionId,
        UUID fileId
) {
}
