package com.fabbitinc.server.application.issue.query.condition;

public record IssueListCondition(
        String search,
        String state,
        int offset,
        int limit
) {
}
