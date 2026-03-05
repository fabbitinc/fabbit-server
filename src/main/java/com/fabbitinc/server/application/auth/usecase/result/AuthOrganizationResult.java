package com.fabbitinc.server.application.auth.usecase.result;

import java.util.UUID;

public record AuthOrganizationResult(
        UUID id,
        String slug,
        String name,
        String industry,
        String teamSize,
        String planType,
        String profileImageUrl
) {
}
