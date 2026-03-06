package com.fabbitinc.server.application.usage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "응답 DTO")
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
