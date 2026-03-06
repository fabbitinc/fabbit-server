package com.fabbitinc.server.application.issue.query.condition;

public record ChangeRequestLookupCondition(
        String search,
        int limit
) {
}
