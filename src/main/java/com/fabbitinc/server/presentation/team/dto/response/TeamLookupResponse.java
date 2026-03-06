package com.fabbitinc.server.presentation.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record TeamLookupResponse(
        List<TeamLookupItemResponse> items
) {
    public record TeamLookupItemResponse(
            UUID id,
            String name
    ) {
    }
}
