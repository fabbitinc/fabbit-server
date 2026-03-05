package com.fabbitinc.server.application.auth.usecase.result;

import java.time.Instant;
import java.util.UUID;

public record AuthUserResult(
        UUID id,
        String email,
        String fullName,
        String phone,
        String profileImageUrl,
        boolean active,
        Instant createdAt
) {
}
