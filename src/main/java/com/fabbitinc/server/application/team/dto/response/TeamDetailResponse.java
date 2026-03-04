package com.fabbitinc.server.application.team.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TeamDetailResponse(
        UUID id,
        String name,
        String description,
        int memberCount,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
