package com.fabbitinc.server.application.user.usecase.command;

public record UpdateProfileCommand(
        String fullName,
        String phone
) {
}
