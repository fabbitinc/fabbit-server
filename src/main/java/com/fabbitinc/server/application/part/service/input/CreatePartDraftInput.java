package com.fabbitinc.server.application.part.service.input;

import java.util.UUID;

public record CreatePartDraftInput(
        UUID partId,
        UUID baseRevisionId,
        String reason
) {
}
