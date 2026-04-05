package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record UpdatePartCategoryCommand(
        UUID categoryId,
        String name,
        String formatPrefix,
        String formatSuffix,
        int digits,
        boolean autoNumberingEnabled
) {
}
