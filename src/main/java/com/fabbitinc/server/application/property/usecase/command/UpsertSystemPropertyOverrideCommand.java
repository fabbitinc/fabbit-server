package com.fabbitinc.server.application.property.usecase.command;

public record UpsertSystemPropertyOverrideCommand(
        String ownerType,
        String propertyKey,
        String displayNameOverride,
        Integer displayOrder,
        Boolean active
) {
}
