package com.fabbitinc.server.application.subscription.usecase.command;

import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;

public record UpdateSubscriptionPlanCommand(
        WorkspacePlanType planType
) {
}
