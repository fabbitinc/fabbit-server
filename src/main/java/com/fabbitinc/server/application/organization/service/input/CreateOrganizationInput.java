package com.fabbitinc.server.application.organization.service.input;

import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import com.fabbitinc.server.domain.subscription.model.SeatType;

public record CreateOrganizationInput(
        String orgName,
        String slug,
        String industry,
        String teamSize,
        WorkspacePlanType planType,
        SeatType ownerSeatType
) {
}
