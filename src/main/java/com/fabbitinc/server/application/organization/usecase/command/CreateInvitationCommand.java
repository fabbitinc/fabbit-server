package com.fabbitinc.server.application.organization.usecase.command;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.subscription.model.SeatType;

public record CreateInvitationCommand(
        String email,
        MembershipRole role,
        SeatType seatType
) {
}
