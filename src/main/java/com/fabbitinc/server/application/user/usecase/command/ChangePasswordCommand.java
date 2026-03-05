package com.fabbitinc.server.application.user.usecase.command;

public record ChangePasswordCommand(
        String currentPassword,
        String newPassword
) {
}
