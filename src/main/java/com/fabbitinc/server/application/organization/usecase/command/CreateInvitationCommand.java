package com.fabbitinc.server.application.organization.usecase.command;

public record CreateInvitationCommand(
        String email,
        String role
) {
}
