package com.fabbitinc.server.presentation.label.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record LabelResponse(
        UUID id,
        String name,
        String description,
        String color,
        Instant createdAt,
        UUID createdBy
) {
}
