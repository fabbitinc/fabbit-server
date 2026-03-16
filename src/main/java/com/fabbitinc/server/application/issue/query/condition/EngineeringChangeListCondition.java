package com.fabbitinc.server.application.issue.query.condition;

public record EngineeringChangeListCondition(
        String search,
        String state,
        String engineeringChangeState,
        int offset,
        int limit
) {
}
