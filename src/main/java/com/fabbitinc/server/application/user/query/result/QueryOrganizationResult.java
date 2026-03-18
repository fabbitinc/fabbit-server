package com.fabbitinc.server.application.user.query.result;

import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.util.UUID;

public record QueryOrganizationResult(
        UUID id,
        String slug,
        String name,
        String industry,
        String teamSize,
        WorkspacePlanType planType,
        String profileImageUrl
) {
}
