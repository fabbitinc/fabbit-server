package com.fabbitinc.server.application.label.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LabelResponse(
        UUID id,
        String name,
        String description,
        String color,
        Instant createdAt,
        UUID createdBy
) {
}
