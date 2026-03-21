package com.fabbitinc.server.application.property.usecase.command;

import java.util.List;

public record ReorderPropertyCommand(
        String ownerType,
        List<ReorderPropertyCommandItem> properties
) {

    public ReorderPropertyCommand {
        properties = properties == null ? List.of() : List.copyOf(properties);
    }
}
