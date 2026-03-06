package com.fabbitinc.server.application.label.usecase.result;

import java.time.Instant;
import java.util.UUID;

public record UpdateLabelResult(
        UUID id,
        String name,
        String description,
        String color,
        Instant createdAt,
        UUID createdBy
) {
}
