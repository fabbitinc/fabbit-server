package com.fabbitinc.server.application.team.query.result;

import java.util.List;
import java.util.UUID;

public record TeamLookupResult(
        List<TeamLookupItemResult> items
) {
    public record TeamLookupItemResult(
            UUID id,
            String name
    ) {
    }
}
