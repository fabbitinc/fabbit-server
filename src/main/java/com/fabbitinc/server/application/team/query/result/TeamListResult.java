package com.fabbitinc.server.application.team.query.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeamListResult(
        List<TeamSummaryResult> items
) {
    public record TeamSummaryResult(
            UUID id,
            String name,
            String description,
            int memberCount,
            UUID createdBy,
            Instant createdAt
    ) {
    }
}
