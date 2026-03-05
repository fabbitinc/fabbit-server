package com.fabbitinc.server.application.project.query.result;

import java.util.UUID;

public record ProjectSummaryResult(
        UUID id,
        String name,
        String description,
        int partCount,
        boolean isArchived
) {
}
