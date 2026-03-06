package com.fabbitinc.server.application.team.usecase.command;

import java.util.UUID;

public record DeleteTeamCommand(
        UUID teamId
) {
}
