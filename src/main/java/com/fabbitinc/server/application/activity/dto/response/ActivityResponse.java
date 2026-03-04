package com.fabbitinc.server.application.activity.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        String action,
        String scope,
        UUID actorId,
        String detail,
        Instant createdAt
) {
}
