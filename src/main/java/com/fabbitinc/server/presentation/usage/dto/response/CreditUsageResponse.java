package com.fabbitinc.server.presentation.usage.dto.response;

import java.time.Instant;
import java.util.List;

public record CreditUsageResponse(
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        int totalCreditsUsed,
        int planCreditsUsed,
        int planCreditsLimit,
        int planCreditsRemaining,
        int bonusCreditsUsed,
        int bonusCreditsRemaining,
        List<CreditCategoryItemResponse> categories
) {
    public record CreditCategoryItemResponse(
            String category,
            int creditsUsed,
            long usageCount
    ) {
    }
}
