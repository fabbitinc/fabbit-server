package com.fabbitinc.server.application.property.usecase.result;

import java.util.UUID;

public record CreatePropertyDefinitionResult(
        UUID propertyDefinitionId,
        String ownerType
) {
}
