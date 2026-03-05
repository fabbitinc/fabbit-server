package com.fabbitinc.server.application.project.usecase.command;

import java.util.UUID;

public record UpdateProjectCommand(
        UUID projectId,
        String name,
        String description
) {
}
