package com.fabbitinc.server.application.auth.query.result;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import java.time.Instant;

public record VerifyInvitationResult(
        String email,
        String organizationName,
        String inviterName,
        MembershipRole role,
        boolean existingUser,
        Instant expiresAt
) {
}
