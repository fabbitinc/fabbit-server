package com.fabbitinc.server.application.property.usecase.result;

public record CreatePropertyDefinitionResult(
        String ownerType,
        String propertyKey
) {
}
