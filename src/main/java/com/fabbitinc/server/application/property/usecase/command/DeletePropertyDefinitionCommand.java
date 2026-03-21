package com.fabbitinc.server.application.property.usecase.command;

public record DeletePropertyDefinitionCommand(
        String ownerType,
        String propertyKey
) {
}
