package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record UpdatePartNumberCategoryCommand(
        UUID categoryId,
        String name,
        String prefix,
        String delimiter,
        int digits
) {
}
