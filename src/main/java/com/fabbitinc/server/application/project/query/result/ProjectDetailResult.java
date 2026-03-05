package com.fabbitinc.server.application.project.query.result;

import java.time.Instant;
import java.util.UUID;

public record ProjectDetailResult(
        UUID id,
        String name,
        String description,
        int partCount,
        boolean isArchived,
        Instant createdAt,
        Instant updatedAt
) {
}
