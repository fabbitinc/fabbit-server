package com.fabbitinc.server.application.auth.usecase.command;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;

public record RegisterCommand(
        String verificationToken,
        String code,
        String password,
        String fullName,
        String orgName,
        String slug,
        String industry,
        String teamSize,
        WorkspacePlanType planType,
        SeatType ownerSeatType
) {
}
