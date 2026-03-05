package com.fabbitinc.server.application.user.usecase.result;

import java.time.Instant;
import java.util.UUID;

public record UpdateProfileResult(
        UUID id,
        String email,
        String fullName,
        String phone,
        String profileImageUrl,
        Instant updatedAt
) {
}
