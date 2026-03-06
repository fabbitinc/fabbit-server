package com.fabbitinc.server.application.issue.query.condition;

public record ChangeRequestListCondition(
        String search,
        String state,
        String crState,
        int offset,
        int limit
) {
}
