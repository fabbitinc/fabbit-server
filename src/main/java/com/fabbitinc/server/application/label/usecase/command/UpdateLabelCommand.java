package com.fabbitinc.server.application.label.usecase.command;

import java.util.UUID;

public record UpdateLabelCommand(
        UUID labelId,
        String name,
        String description,
        String color,
        boolean descriptionSet
) {
}
