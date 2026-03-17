package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.List;
import java.util.UUID;

public record PartListResult(
        String nextCursor,
        String prevCursor,
        List<Item> items
) {
    public record Item(
            UUID id,
            UUID revisionId,
            String partNumber,
            String name,
            String category,
            String revisionCode,
            PartRevisionStatus revisionStatus,
            PartLifecycleState lifecycleState,
            boolean hasDrawing,
            long childrenCount
    ) {
    }
}
