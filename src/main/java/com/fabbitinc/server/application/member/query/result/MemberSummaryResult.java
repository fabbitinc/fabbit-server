package com.fabbitinc.server.application.member.query.result;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import java.util.UUID;

public record MemberSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        MembershipRole role,
        String jobRole,
        SeatType seatType
) {
}
