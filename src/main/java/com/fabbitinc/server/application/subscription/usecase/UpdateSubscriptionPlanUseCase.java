package com.fabbitinc.server.application.subscription.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.usecase.command.UpdateSubscriptionPlanCommand;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpdateSubscriptionPlanUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final SubscriptionApi subscriptionApi;

    @PreAuthorize("hasRole('ADMIN')")
    public void execute(UpdateSubscriptionPlanCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        subscriptionApi.schedulePlanChangeAtCurrentPeriodEnd(
                auth.orgId(),
                command.planType(),
                Map.of("actorUserId", auth.userId())
        );
        log.atInfo()
                .addKeyValue("event.name", "subscription.plan.change.scheduled")
                .addKeyValue("organization.id", auth.orgId())
                .addKeyValue("actor.user.id", auth.userId())
                .addKeyValue("requested.planType", command.planType())
                .addKeyValue("outcome", "success")
                .log("subscription plan change scheduled");
    }
}
