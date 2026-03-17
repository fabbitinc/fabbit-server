package com.fabbitinc.server.application.property.usecase.result;

import java.util.UUID;

public record UpdatePropertyDefinitionResult(
        UUID propertyDefinitionId,
        String ownerType
) {
}
