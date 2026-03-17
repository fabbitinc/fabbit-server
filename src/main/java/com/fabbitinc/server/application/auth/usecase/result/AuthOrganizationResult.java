package com.fabbitinc.server.application.auth.usecase.result;

import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.util.UUID;

public record AuthOrganizationResult(
        UUID id,
        String slug,
        String name,
        String industry,
        String teamSize,
        WorkspacePlanType planType,
        String profileImageUrl
) {
}
