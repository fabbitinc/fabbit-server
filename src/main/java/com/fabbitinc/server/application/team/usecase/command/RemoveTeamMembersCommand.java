package com.fabbitinc.server.application.team.usecase.command;

import java.util.List;
import java.util.UUID;

public record RemoveTeamMembersCommand(
        UUID teamId,
        List<UUID> userIds
) {
}
