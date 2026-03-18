package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.Subscription;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByOrgId(UUID orgId);

    Optional<Subscription> findByOrgIdAndStatus(UUID orgId, SubscriptionStatus status);

    List<Subscription> findByStatusAndScheduledChangeEffectiveAtLessThanEqual(SubscriptionStatus status, Instant effectiveAt);

    List<Subscription> findByStatusInAndCurrentPeriodEndLessThanEqual(List<SubscriptionStatus> statuses, Instant currentPeriodEnd);
}
