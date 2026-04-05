package com.fabbitinc.server.application.part.usecase.command;

public record CreatePartCategoryCommand(
        String name,
        String prefix,
        String delimiter,
        int digits
) {
}
