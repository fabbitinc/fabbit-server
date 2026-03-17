package com.fabbitinc.server.application.organization.usecase.result;

import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.util.UUID;

public record CreateOrganizationResult(
        UUID organizationId,
        String organizationSlug,
        String organizationName,
        String organizationIndustry,
        String organizationTeamSize,
        WorkspacePlanType organizationPlanType,
        String organizationProfileImageUrl,
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
