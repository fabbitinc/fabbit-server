package com.fabbitinc.server.application.auth.query.result;

public record PlanResult(
        String planType,
        String displayName,
        String description,
        int maxMembers,
        int storageGb,
        int aiCredits,
        int priceMonthly
) {
}
