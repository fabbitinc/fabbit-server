package com.fabbitinc.server.application.subscription.service;

import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.subscription.model.Subscription;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final long GB_TO_BYTES = 1_000_000_000L;

    private final SubscriptionRepository subscriptionRepository;

    public Subscription createInitialSubscription(UUID orgId, PlanType planType) {
        return subscriptionRepository.findByOrgIdAndStatus(orgId, SubscriptionStatus.ACTIVE)
                .orElseGet(() -> subscriptionRepository.save(createActiveSubscription(orgId, planType)));
    }

    private Subscription createActiveSubscription(UUID orgId, PlanType planType) {
        PlanType resolvedPlanType = PlanType.defaultIfNull(planType);
        Instant now = Instant.now();
        Instant periodEnd = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
                .plusMonths(1)
                .toInstant();

        return Subscription.create(
                orgId,
                resolvedPlanType.name(),
                SubscriptionStatus.ACTIVE,
                now,
                periodEnd,
                resolvedPlanType.maxMembers(),
                resolvedPlanType.aiCredits(),
                (long) resolvedPlanType.storageGb() * GB_TO_BYTES
        );
    }
}
