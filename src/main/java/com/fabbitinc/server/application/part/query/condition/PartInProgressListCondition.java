package com.fabbitinc.server.application.part.query.condition;

import java.util.List;
import java.util.UUID;

public record PartInProgressListCondition(
        String search,
        String category,
        String lifecycleState,
        List<PartInProgressStatusFilter> statuses,
        boolean mineOnly,
        Boolean hasDrawing,
        Boolean hasChildren,
        UUID projectId,
        String nextCursor,
        String prevCursor,
        int limit
) {
}
