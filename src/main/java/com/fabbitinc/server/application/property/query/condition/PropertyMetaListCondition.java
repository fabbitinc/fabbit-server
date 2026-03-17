package com.fabbitinc.server.application.property.query.condition;

public record PropertyMetaListCondition(
        String ownerType,
        boolean includeInactive
) {
}
