package com.fabbitinc.server.application.organization.usecase.result;

import java.time.Instant;
import java.util.UUID;

public record SwitchOrganizationResult(
        UUID userId,
        String userEmail,
        String userFullName,
        String userPhone,
        String userProfileImageUrl,
        boolean userActive,
        Instant userCreatedAt,
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
