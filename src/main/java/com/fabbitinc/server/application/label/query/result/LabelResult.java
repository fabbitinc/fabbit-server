package com.fabbitinc.server.application.label.query.result;

import java.time.Instant;
import java.util.UUID;

public record LabelResult(
        UUID id,
        String name,
        String description,
        String color,
        Instant createdAt,
        UUID createdBy
) {
}
