package com.fabbitinc.server.application.organization.usecase.command;

import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import com.fabbitinc.server.domain.subscription.model.SeatType;

public record CreateOrganizationCommand(
        String orgName,
        String slug,
        String industry,
        String teamSize,
        WorkspacePlanType planType,
        SeatType ownerSeatType
) {
}
