package com.fabbitinc.server.application.subscription.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.usecase.command.UpdateSubscriptionSeatQuotasCommand;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UpdateSubscriptionSeatQuotasUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final SubscriptionApi subscriptionApi;

    @PreAuthorize("hasRole('ADMIN')")
    public void execute(UpdateSubscriptionSeatQuotasCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Map<SeatType, Integer> requestedQuantities = command.seatQuantities().stream()
                .collect(Collectors.toMap(
                        UpdateSubscriptionSeatQuotasCommand.SeatQuantityCommand::seatType,
                        UpdateSubscriptionSeatQuotasCommand.SeatQuantityCommand::purchasedQuantity,
                        (left, right) -> right
                ));
        subscriptionApi.updateSeatQuotas(auth.orgId(), requestedQuantities);
    }
}
