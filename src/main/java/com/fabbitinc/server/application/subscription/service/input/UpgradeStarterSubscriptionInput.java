package com.fabbitinc.server.application.subscription.service.input;

import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.util.List;
import java.util.UUID;

public record UpgradeStarterSubscriptionInput(
        UUID orgId,
        WorkspacePlanType targetPlanType,
        List<MemberSeatSelection> memberSeatSelections,
        UUID actorUserId
) {

    public record MemberSeatSelection(
            Membership membership,
            SeatType seatType
    ) {
    }
}
