package com.fabbitinc.server.application.subscription.usecase.command;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import java.util.List;

public record UpdateSubscriptionSeatQuotasCommand(
        List<SeatQuantityCommand> seatQuantities
) {
    public record SeatQuantityCommand(
            SeatType seatType,
            int purchasedQuantity
    ) {
    }
}
