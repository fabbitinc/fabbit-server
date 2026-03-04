package com.fabbitinc.server.domain.organization.model;

public record PlanLimits(
        int maxMembers,
        int storageGb,
        int aiCredits,
        int priceMonthly,
        String displayName,
        String description
) {
}
