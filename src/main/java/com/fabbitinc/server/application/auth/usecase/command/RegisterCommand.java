package com.fabbitinc.server.application.auth.usecase.command;

public record RegisterCommand(
        String verificationToken,
        String code,
        String password,
        String fullName,
        String orgName,
        String slug,
        String industry,
        String teamSize,
        String planType
) {
}
