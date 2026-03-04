package com.fabbitinc.server.application.project.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProjectDetailResponse(
        UUID id,
        String name,
        String description,
        int partCount,
        boolean isArchived,
        Instant createdAt,
        Instant updatedAt
) {
}
