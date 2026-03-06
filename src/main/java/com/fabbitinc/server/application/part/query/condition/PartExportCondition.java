package com.fabbitinc.server.application.part.query.condition;

import java.util.List;
import java.util.UUID;

public record PartExportCondition(
        String search,
        String category,
        String lifecycleState,
        Boolean hasDrawing,
        Boolean hasChildren,
        List<UUID> partIds,
        UUID mappingId,
        UUID projectId
) {
}
