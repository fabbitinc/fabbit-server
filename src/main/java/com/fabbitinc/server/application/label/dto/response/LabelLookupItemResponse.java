package com.fabbitinc.server.application.label.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record LabelLookupItemResponse(
        UUID id,
        String name,
        String color
) {
}
