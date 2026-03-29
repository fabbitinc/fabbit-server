package com.fabbitinc.server.application.bom.usecase.command;

import java.util.UUID;

public record DeleteBomItemCommand(
        UUID partId,
        UUID revisionId,
        UUID bomItemId
) {
}
