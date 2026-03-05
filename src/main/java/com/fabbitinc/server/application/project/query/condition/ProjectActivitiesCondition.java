package com.fabbitinc.server.application.project.query.condition;

import com.fabbitinc.server.application.activity.dto.response.ActivityScope;

import java.util.UUID;

public record ProjectActivitiesCondition(
        UUID projectId,
        UUID cursor,
        int limit,
        ActivityScope scope,
        UUID userId
) {
}
