package com.fabbitinc.server.application.auth.usecase.command;

public record AcceptInvitationCommand(
        String token,
        String password,
        String fullName
) {
}
