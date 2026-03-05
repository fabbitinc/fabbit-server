package com.fabbitinc.server.application.organization.usecase.result;

import java.util.UUID;

public record CreateOrganizationResult(
        UUID organizationId,
        String organizationSlug,
        String organizationName,
        String organizationIndustry,
        String organizationTeamSize,
        String organizationPlanType,
        String organizationProfileImageUrl,
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
