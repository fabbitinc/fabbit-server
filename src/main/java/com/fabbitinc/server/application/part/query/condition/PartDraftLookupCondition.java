package com.fabbitinc.server.application.part.query.condition;

public record PartDraftLookupCondition(
        String search,
        int limit
) {
}
