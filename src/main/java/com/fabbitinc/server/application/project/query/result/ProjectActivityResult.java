package com.fabbitinc.server.application.project.query.result;

import java.time.Instant;
import java.util.UUID;

public record ProjectActivityResult(
        UUID id,
        String action,
        String scope,
        UUID actorId,
        String detail,
        Instant createdAt
) {
}
