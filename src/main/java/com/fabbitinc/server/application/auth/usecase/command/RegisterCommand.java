package com.fabbitinc.server.application.auth.usecase.command;

import com.fabbitinc.server.domain.organization.model.PlanType;

public record RegisterCommand(
        String verificationToken,
        String code,
        String password,
        String fullName,
        String orgName,
        String slug,
        String industry,
        String teamSize,
        PlanType planType
) {
}
