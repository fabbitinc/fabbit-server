package com.fabbitinc.server.application.property.usecase.command;

import java.util.UUID;

public record DeletePropertyDefinitionCommand(
        UUID propertyDefinitionId
) {
}
