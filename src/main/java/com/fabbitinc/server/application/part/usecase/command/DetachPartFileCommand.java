package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record DetachPartFileCommand(
        String partNumber,
        String revisionCode,
        String baseRevisionCode,
        String draftKey,
        UUID fileId
) {
}
