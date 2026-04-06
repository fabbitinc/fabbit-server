package com.fabbitinc.server.application.part.query.condition;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;

public record PartRevisionLookupCondition(
        String search,
        int limit,
        PartRevisionStatus status,
        boolean mineOnly
) {
}
