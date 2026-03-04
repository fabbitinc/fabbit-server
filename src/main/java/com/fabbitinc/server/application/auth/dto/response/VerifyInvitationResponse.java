package com.fabbitinc.server.application.auth.dto.response;

import java.time.Instant;

public record VerifyInvitationResponse(
        String email,
        String orgName,
        String inviterName,
        String role,
        boolean isExistingUser,
        Instant expiresAt
) {
}
