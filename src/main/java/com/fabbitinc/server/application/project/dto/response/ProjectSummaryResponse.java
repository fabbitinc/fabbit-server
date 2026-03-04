package com.fabbitinc.server.application.project.dto.response;

import java.util.UUID;

public record ProjectSummaryResponse(
        UUID id,
        String name,
        String description,
        int partCount,
        boolean isArchived
) {
}
