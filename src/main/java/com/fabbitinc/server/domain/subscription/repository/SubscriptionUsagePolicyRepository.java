package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.SubscriptionUsagePolicy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionUsagePolicyRepository extends JpaRepository<SubscriptionUsagePolicy, UUID> {

    Optional<SubscriptionUsagePolicy> findBySubscriptionId(UUID subscriptionId);
}
