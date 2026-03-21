package com.fabbitinc.server.application.property.usecase.command;

public record ReorderPropertyCommandItem(
        String propertyKey,
        boolean system
) {
}
