package com.fabbitinc.server.application.bom.usecase.command;

import java.util.UUID;

public record PreviewBomImportCommand(
        UUID partId,
        UUID revisionId,
        UUID fileId
) {
}
