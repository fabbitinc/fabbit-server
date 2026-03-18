package com.fabbitinc.server.application.member.usecase.command;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import java.util.UUID;

public record ChangeMemberSeatCommand(
        UUID userId,
        SeatType seatType
) {
}
