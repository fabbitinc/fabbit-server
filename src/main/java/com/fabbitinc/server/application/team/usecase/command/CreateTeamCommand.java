package com.fabbitinc.server.application.team.usecase.command;

public record CreateTeamCommand(
        String name,
        String description
) {
}
