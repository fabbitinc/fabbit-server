package com.fabbitinc.server.application.auth.query.result;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import java.time.Instant;

public record VerifyInvitationResult(
        String email,
        String organizationName,
        String inviterName,
        MembershipRole role,
        SeatType seatType,
        boolean existingUser,
        Instant expiresAt
) {
}
