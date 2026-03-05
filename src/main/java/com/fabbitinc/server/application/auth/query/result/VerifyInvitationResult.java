package com.fabbitinc.server.application.auth.query.result;

import java.time.Instant;

public record VerifyInvitationResult(
        String email,
        String organizationName,
        String inviterName,
        String role,
        boolean existingUser,
        Instant expiresAt
) {
}
