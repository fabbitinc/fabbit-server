package com.fabbitinc.server.application.team.usecase.command;

import java.util.UUID;

public record UpdateTeamCommand(
        UUID teamId,
        String name,
        String description
) {
}
