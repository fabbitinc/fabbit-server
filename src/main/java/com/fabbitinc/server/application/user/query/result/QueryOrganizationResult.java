package com.fabbitinc.server.application.user.query.result;

import com.fabbitinc.server.domain.organization.model.PlanType;

import java.util.UUID;

public record QueryOrganizationResult(
        UUID id,
        String slug,
        String name,
        String industry,
        String teamSize,
        PlanType planType,
        String profileImageUrl
) {
}
