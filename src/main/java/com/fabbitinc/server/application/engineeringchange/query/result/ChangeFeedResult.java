package com.fabbitinc.server.application.engineeringchange.query.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChangeFeedResult(List<ChangeFeedItem> items) {

    public record ChangeFeedItem(
            UUID ecId,
            int ecNumber,
            String title,
            List<String> affectedPartNumbers,
            int affectedPartCount,
            Instant releasedAt,
            UUID releasedById,
            String releasedByName,
            Integer sourceIssueNumber
    ) {
    }
}
