package com.fabbitinc.server.application.project.query.result;

import java.util.UUID;

public record PartProjectSummaryResult(
        UUID id,
        String name,
        String description
) {
}
