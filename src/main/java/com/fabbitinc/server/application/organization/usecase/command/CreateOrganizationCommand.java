package com.fabbitinc.server.application.organization.usecase.command;

import com.fabbitinc.server.domain.organization.model.PlanType;

public record CreateOrganizationCommand(
        String orgName,
        String slug,
        String industry,
        String teamSize,
        PlanType planType
) {
}
