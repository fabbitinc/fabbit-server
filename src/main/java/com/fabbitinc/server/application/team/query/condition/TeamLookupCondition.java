package com.fabbitinc.server.application.team.query.condition;

public record TeamLookupCondition(
        String search,
        int limit
) {
}
