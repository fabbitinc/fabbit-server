package com.fabbitinc.server.application.label.query.condition;

public record LabelLookupCondition(
        String search,
        int limit
) {
}
