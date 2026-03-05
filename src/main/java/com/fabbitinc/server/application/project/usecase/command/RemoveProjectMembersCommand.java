package com.fabbitinc.server.application.project.usecase.command;

import java.util.List;
import java.util.UUID;

public record RemoveProjectMembersCommand(
        UUID projectId,
        List<UUID> userIds
) {
}
