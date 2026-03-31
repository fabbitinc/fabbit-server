package com.fabbitinc.server.application.migration.usecase.command;

import java.util.UUID;

public record PreviewInventorMigrationCommand(
        UUID sessionId
) {
}
