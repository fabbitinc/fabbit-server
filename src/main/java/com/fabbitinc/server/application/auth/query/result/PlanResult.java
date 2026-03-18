package com.fabbitinc.server.application.auth.query.result;

import com.fabbitinc.server.domain.subscription.model.AiBillingMode;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;

public record PlanResult(
        WorkspacePlanType planType,
        String displayName,
        String description,
        int maxMembers,
        long baseStorageBytes,
        long extraStorageBytesPerFullSeat,
        int starterMonthlyAiCredits,
        AiBillingMode aiBillingMode,
        int viewerMonthlyPrice,
        int collaboratorMonthlyPrice,
        int fullSeatMonthlyPrice,
        int storageOveragePricePerGb
) {
}
