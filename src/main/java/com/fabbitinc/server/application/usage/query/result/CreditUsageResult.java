package com.fabbitinc.server.application.usage.query.result;

import java.time.Instant;
import java.util.List;

public record CreditUsageResult(
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        int totalCreditsUsed,
        int includedCreditsLimit,
        int includedCreditsUsed,
        int includedCreditsRemaining,
        Integer meteredCreditsLimit,
        boolean hardLimitEnabled,
        List<CreditCategoryItemResult> categories
) {
    public record CreditCategoryItemResult(
            String category,
            int creditsUsed,
            long usageCount
    ) {
    }
}
