package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record PartListCondition(
        String search,
        String category,
        String lifecycleState,
        Boolean hasDrawing,
        Boolean hasChildren,
        UUID projectId,
        String nextCursor,
        String prevCursor,
        int limit
) {
}
