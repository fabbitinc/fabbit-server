package com.fabbitinc.server.application.organization.query.result;

import java.time.Instant;
import java.util.UUID;

public record OrganizationInvitationResult(
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
