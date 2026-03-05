package com.fabbitinc.server.application.auth.query.result;

import com.fabbitinc.server.domain.organization.model.PlanType;

public record PlanResult(
        PlanType planType,
        String displayName,
        String description,
        int maxMembers,
        int storageGb,
        int aiCredits,
        int priceMonthly
) {
}
