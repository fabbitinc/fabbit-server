package com.fabbitinc.server.application.bom.usecase.command;

import java.util.UUID;

public record CommitBomImportCommand(
        UUID partId,
        UUID revisionId,
        UUID fileId,
        BomImportMode mode
) {

    public enum BomImportMode {
        APPEND,
        REPLACE
    }
}
