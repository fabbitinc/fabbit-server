package com.fabbitinc.server.application.user.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UpdateProfileResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String profileImageUrl,
        Instant updatedAt
) {
}
