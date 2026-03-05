package com.fabbitinc.server.application.project.usecase.command;

import java.util.List;
import java.util.UUID;

public record UnlinkProjectPartsCommand(
        UUID projectId,
        List<UUID> partIds
) {
}
