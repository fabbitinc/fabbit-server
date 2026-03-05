package com.fabbitinc.server.application.user.query.result;

import java.time.Instant;
import java.util.UUID;

public record QueryUserResult(
        UUID id,
        String email,
        String fullName,
        String phone,
        String profileImageUrl,
        boolean active,
        Instant createdAt
) {
}
