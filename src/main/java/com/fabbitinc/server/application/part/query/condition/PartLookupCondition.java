package com.fabbitinc.server.application.part.query.condition;

public record PartLookupCondition(
        String search,
        int limit
) {
}
