package com.fabbitinc.server.application.part.usecase.command;

public record CreatePartCategoryCommand(
        String name,
        String formatPrefix,
        String formatSuffix,
        int digits,
        boolean autoNumberingEnabled
) {
}
