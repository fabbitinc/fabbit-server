package com.fabbitinc.server.application.label.usecase.command;

public record CreateLabelCommand(
        String name,
        String description,
        String color
) {
}
