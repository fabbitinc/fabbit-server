package com.fabbitinc.server.application.project.usecase.command;

import com.fabbitinc.server.domain.project.model.ProjectRole;
import java.util.List;
import java.util.UUID;

public record AddProjectMembersCommand(
        UUID projectId,
        List<UUID> userIds,
        ProjectRole role
) {
}
