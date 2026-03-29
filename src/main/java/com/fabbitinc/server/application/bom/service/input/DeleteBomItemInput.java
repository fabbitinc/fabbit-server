package com.fabbitinc.server.application.bom.service.input;

import java.util.UUID;

public record DeleteBomItemInput(
        UUID partId,
        UUID revisionId,
        UUID bomItemId
) {
}
