package com.fabbitinc.server.application.part.dto.response;

import java.util.UUID;

public record PartProjectSummaryResponse(
        UUID id,
        String name,
        String description
) {
}
