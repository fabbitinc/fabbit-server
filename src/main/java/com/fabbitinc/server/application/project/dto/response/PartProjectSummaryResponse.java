package com.fabbitinc.server.application.project.dto.response;

import java.util.UUID;

public record PartProjectSummaryResponse(
        UUID id,
        String name,
        String description
) {
}
