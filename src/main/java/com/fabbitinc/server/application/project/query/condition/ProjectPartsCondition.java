package com.fabbitinc.server.application.project.query.condition;

import java.util.UUID;

public record ProjectPartsCondition(
        UUID projectId,
        String search,
        int offset,
        int limit
) {
}
