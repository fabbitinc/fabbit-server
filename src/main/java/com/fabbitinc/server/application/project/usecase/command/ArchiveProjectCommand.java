package com.fabbitinc.server.application.project.usecase.command;

import java.util.UUID;

public record ArchiveProjectCommand(
        UUID projectId
) {
}
