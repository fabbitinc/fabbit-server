package com.fabbitinc.server.application.organization.usecase.result;

import com.fabbitinc.server.domain.auth.model.InvitationStatus;
import com.fabbitinc.server.domain.organization.model.MembershipRole;

import java.time.Instant;
import java.util.UUID;

public record CreateInvitationResult(
        UUID id,
        UUID orgId,
        String email,
        MembershipRole role,
        InvitationStatus status,
        UUID invitedBy,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt
) {
}
