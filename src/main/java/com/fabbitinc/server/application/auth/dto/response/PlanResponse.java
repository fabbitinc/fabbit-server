package com.fabbitinc.server.application.auth.dto.response;

public record PlanResponse(
        String planType,
        String displayName,
        String description,
        int maxMembers,
        int storageGb,
        int aiCredits,
        int priceMonthly
) {
}
