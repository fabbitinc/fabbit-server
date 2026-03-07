package com.fabbitinc.server.application.subscription.api;

import com.fabbitinc.server.application.subscription.service.SubscriptionService;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.subscription.model.Subscription;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionApi {

    private final SubscriptionService subscriptionService;

    public Subscription createInitialSubscription(UUID orgId, PlanType planType) {
        return subscriptionService.createInitialSubscription(orgId, planType);
    }
}
