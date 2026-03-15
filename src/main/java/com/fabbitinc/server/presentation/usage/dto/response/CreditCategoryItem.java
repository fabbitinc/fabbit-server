package com.fabbitinc.server.presentation.usage.dto.response;

public record CreditCategoryItem(
        String category,
        int creditsUsed,
        long usageCount
) {
}
