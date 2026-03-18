package com.fabbitinc.server.application.subscription.usecase;

import com.fabbitinc.server.application.subscription.service.SubscriptionService;
import com.fabbitinc.server.application.subscription.usecase.result.ApplyScheduledSubscriptionChangesResult;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ApplyScheduledSubscriptionChangesUseCase {

    private final SubscriptionService subscriptionService;

    public ApplyScheduledSubscriptionChangesResult execute() {
        SubscriptionService.PlanChangeExecutionResult result = subscriptionService.applyDueScheduledPlanChanges(Instant.now());
        log.atInfo()
                .addKeyValue("event.name", "subscription.plan.change.batch.completed")
                .addKeyValue("subscription.appliedCount", result.appliedCount())
                .addKeyValue("subscription.failedCount", result.failedCount())
                .addKeyValue("outcome", "success")
                .log("scheduled subscription plan changes applied");
        return new ApplyScheduledSubscriptionChangesResult(result.appliedCount(), result.failedCount());
    }
}
