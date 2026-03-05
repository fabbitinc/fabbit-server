package com.fabbitinc.server.application.organization.usecase.result;

import java.time.Instant;
import java.util.UUID;

public record CreateInvitationResult(
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
