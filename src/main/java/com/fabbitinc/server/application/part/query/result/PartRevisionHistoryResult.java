package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionHistoryActionType;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartRevisionHistoryResult(
        List<Item> items
) {

    public record Item(
            UUID revisionId,
            String revisionCode,
            PartRevisionStatus status,
            String name,
            Instant createdAt,
            PartUserSummaryResult createdBy,
            PartRevisionDiffSummaryResult summary,
            List<Entry> entries
    ) {
    }

    public record Entry(
            PartRevisionHistoryActionType actionType,
            Instant occurredAt,
            PartUserSummaryResult actor,
            String reason
    ) {
    }
}
