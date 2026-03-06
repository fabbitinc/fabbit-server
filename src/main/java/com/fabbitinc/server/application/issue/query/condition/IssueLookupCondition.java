package com.fabbitinc.server.application.issue.query.condition;

public record IssueLookupCondition(
        String search,
        String type,
        int limit
) {
}
