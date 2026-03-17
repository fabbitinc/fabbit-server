package com.fabbitinc.server.application.property.query.condition;

public record PropertyMetaCondition(
        String ownerType,
        String propertyKey,
        boolean includeInactive
) {
}
