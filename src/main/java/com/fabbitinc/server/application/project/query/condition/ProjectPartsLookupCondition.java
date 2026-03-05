package com.fabbitinc.server.application.project.query.condition;

import java.util.UUID;

public record ProjectPartsLookupCondition(
        UUID projectId,
        String search,
        boolean excludeLinked,
        int limit
) {
}
