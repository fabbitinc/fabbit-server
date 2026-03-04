package com.fabbitinc.server.application.auth.dto.response;

import java.util.UUID;
import java.time.Instant;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String profileImageUrl,
        boolean isActive,
        Instant createdAt
) {
}
