package com.fabbitinc.server.application.usage.query.result;

import java.time.Instant;
import java.util.List;

public record CreditUsageResult(
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        int totalCreditsUsed,
        int planCreditsUsed,
        int planCreditsLimit,
        int planCreditsRemaining,
        int bonusCreditsUsed,
        int bonusCreditsRemaining,
        List<CreditCategoryItemResult> categories
) {
    public record CreditCategoryItemResult(
            String category,
            int creditsUsed,
            long usageCount
    ) {
    }
}
