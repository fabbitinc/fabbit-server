package com.fabbitinc.server.presentation.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record TeamLookupItemResponse(
        UUID id,
        String name
) {
}
