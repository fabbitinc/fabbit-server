package com.fabbitinc.server.application.usage.dto.response;

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
        List<CreditCategoryItem> categories
) {
}
