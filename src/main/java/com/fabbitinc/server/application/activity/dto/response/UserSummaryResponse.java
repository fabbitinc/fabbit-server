package com.fabbitinc.server.application.activity.dto.response;

import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
