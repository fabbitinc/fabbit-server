package com.fabbitinc.server.application.auth.usecase.command;

public record LoginCommand(
        String email,
        String password,
        String slug
) {
}
