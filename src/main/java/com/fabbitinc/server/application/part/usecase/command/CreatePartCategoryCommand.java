package com.fabbitinc.server.application.part.usecase.command;

public record CreatePartCategoryCommand(
        String name,
        String formatPrefix,
        String formatSuffix,
        Integer digits,
        boolean autoNumberingEnabled
) {
}
