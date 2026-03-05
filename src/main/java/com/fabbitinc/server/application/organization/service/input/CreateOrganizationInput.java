package com.fabbitinc.server.application.organization.service.input;

import com.fabbitinc.server.domain.organization.model.PlanType;

public record CreateOrganizationInput(
        String orgName,
        String slug,
        String industry,
        String teamSize,
        PlanType planType
) {
}
