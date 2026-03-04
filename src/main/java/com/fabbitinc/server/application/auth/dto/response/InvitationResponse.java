package com.fabbitinc.server.application.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID orgId,
        String email,
        String role,
        String status,
        UUID invitedBy,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt
) {
}
