package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.List;
import java.util.UUID;

public record PartListResult(
        long total,
        int offset,
        int limit,
        List<Item> items
) {
    public record Item(
            UUID id,
            String partNumber,
            String name,
            String category,
            String revision,
            PartLifecycleState lifecycleState,
            UUID drawingId,
            long childrenCount
    ) {
    }
}
