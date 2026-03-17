package com.fabbitinc.server.application.property.usecase.result;

public record UpsertSystemPropertyOverrideResult(
        String ownerType,
        String propertyKey
) {
}
