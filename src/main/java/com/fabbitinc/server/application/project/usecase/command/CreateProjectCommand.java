package com.fabbitinc.server.application.project.usecase.command;

public record CreateProjectCommand(
        String name,
        String description
) {
}
