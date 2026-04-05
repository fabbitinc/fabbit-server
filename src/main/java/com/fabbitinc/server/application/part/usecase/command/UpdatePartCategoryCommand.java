package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record UpdatePartCategoryCommand(
        UUID categoryId,
        String name,
        String prefix,
        String delimiter,
        int digits
) {
}
