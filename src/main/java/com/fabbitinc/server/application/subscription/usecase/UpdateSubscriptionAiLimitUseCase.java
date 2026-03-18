package com.fabbitinc.server.application.subscription.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.usecase.command.UpdateSubscriptionAiLimitCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpdateSubscriptionAiLimitUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final SubscriptionApi subscriptionApi;

    @PreAuthorize("hasRole('ADMIN')")
    public void execute(UpdateSubscriptionAiLimitCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        subscriptionApi.updateAiLimit(auth.orgId(), command.aiMonthlyCreditLimit(), command.aiHardLimitEnabled());
        log.atInfo()
                .addKeyValue("event.name", "subscription.ai.limit.updated")
                .addKeyValue("organization.id", auth.orgId())
                .addKeyValue("actor.user.id", auth.userId())
                .addKeyValue("ai.monthlyCreditLimit", command.aiMonthlyCreditLimit())
                .addKeyValue("ai.hardLimitEnabled", command.aiHardLimitEnabled())
                .addKeyValue("outcome", "success")
                .log("subscription ai limit updated");
    }
}
