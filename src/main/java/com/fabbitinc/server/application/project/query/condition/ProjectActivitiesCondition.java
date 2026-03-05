package com.fabbitinc.server.application.project.query.condition;

import java.util.UUID;

public record ProjectActivitiesCondition(
        UUID projectId,
        UUID cursor,
        int limit,
        String scope,
        UUID userId
) {
}
