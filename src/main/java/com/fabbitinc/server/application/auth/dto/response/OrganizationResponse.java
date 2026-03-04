package com.fabbitinc.server.application.auth.dto.response;

import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String slug,
        String name,
        String industry,
        String teamSize,
        String planType,
        String profileImageUrl
) {
}
