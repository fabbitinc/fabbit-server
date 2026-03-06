package com.fabbitinc.server.application.team.query.result;

import java.time.Instant;
import java.util.UUID;

public record TeamDetailResult(
        UUID id,
        String name,
        String description,
        int memberCount,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
