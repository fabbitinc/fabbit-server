package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartInProgressListResult(
        String nextCursor,
        String prevCursor,
        List<Item> items
) {
    public record Item(
            UUID partId,
            UUID revisionId,
            String partNumber,
            String name,
            String category,
            PartRevisionStatus status,
            String revisionCode,
            String draftKey,
            String baseRevisionCode,
            PartLifecycleState lifecycleState,
            boolean hasDrawing,
            long childrenCount,
            Instant updatedAt
    ) {
    }
}
