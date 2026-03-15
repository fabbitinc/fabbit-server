package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record PartProjectSummaryResponse(
        UUID id,
        String name,
        String description
) {
}
