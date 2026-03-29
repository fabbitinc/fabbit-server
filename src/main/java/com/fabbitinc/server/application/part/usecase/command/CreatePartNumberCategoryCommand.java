package com.fabbitinc.server.application.part.usecase.command;

public record CreatePartNumberCategoryCommand(
        String name,
        String prefix,
        String delimiter,
        int digits
) {
}
