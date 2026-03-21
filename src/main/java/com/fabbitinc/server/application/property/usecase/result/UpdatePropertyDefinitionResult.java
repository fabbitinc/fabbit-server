package com.fabbitinc.server.application.property.usecase.result;

public record UpdatePropertyDefinitionResult(
        String ownerType,
        String propertyKey
) {
}
