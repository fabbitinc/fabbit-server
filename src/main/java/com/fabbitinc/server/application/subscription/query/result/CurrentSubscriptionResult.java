package com.fabbitinc.server.application.subscription.query.result;

import com.fabbitinc.server.domain.subscription.model.AiBillingMode;
import com.fabbitinc.server.domain.subscription.model.BillingCycle;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.time.Instant;
import java.util.List;

public record CurrentSubscriptionResult(
        WorkspacePlanType planType,
        SubscriptionStatus status,
        BillingCycle billingCycle,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        WorkspacePlanType scheduledPlanType,
        Instant scheduledChangeEffectiveAt,
        int usedMembers,
        long storageBytesUsed,
        long storageBytesIncluded,
        long storageBytesOverage,
        boolean allowStorageOverage,
        AiBillingMode aiBillingMode,
        int starterMonthlyAiCredits,
        Integer aiMonthlyCreditLimit,
        boolean aiHardLimitEnabled,
        List<SeatAllocationResult> seatAllocations
) {
    public record SeatAllocationResult(
            SeatType seatType,
            int assignedCount,
            int purchasedQuantity
    ) {
    }
}
