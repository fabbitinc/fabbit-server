package com.fabbitinc.server.application.part.query.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartChangeHistoryResult(List<ChangeHistoryItem> items) {

    public record ChangeHistoryItem(
            Instant timestamp,
            String type,
            UUID referenceId,
            int referenceNumber,
            String title,
            String actorName
    ) {
    }
}
