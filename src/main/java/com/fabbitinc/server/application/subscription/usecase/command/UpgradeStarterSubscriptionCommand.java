package com.fabbitinc.server.application.subscription.usecase.command;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.util.List;
import java.util.UUID;

public record UpgradeStarterSubscriptionCommand(
        WorkspacePlanType targetPlanType,
        List<MemberSeatCommand> memberSeats
) {

    public record MemberSeatCommand(
            UUID membershipId,
            SeatType seatType
    ) {
    }
}
