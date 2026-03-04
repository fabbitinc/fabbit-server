package com.fabbitinc.server.application.usage.dto.response;

public record CreditCategoryItem(
        String category,
        int creditsUsed,
        long usageCount
) {
}
