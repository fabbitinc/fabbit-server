package com.fabbitinc.server.application.auth.usecase.result;

import com.fabbitinc.server.domain.organization.model.PlanType;
import java.util.UUID;

public record AuthOrganizationResult(
        UUID id,
        String slug,
        String name,
        String industry,
        String teamSize,
        PlanType planType,
        String profileImageUrl
) {
}
