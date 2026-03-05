package com.fabbitinc.server.application.user.query.result;

import java.util.UUID;

public record QueryOrganizationResult(
        UUID id,
        String slug,
        String name,
        String industry,
        String teamSize,
        String planType,
        String profileImageUrl
) {
}
