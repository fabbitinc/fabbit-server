package com.fabbitinc.server.application.part.service.input;

import java.util.UUID;

public record PartRevisionDecisionInput(
        UUID partId,
        UUID revisionId,
        String reason
) {
}
