package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.SubscriptionCreditPurchase;
import com.fabbitinc.server.domain.subscription.model.SubscriptionCreditPurchaseStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionCreditPurchaseRepository extends JpaRepository<SubscriptionCreditPurchase, UUID> {

    List<SubscriptionCreditPurchase> findByOrgIdAndStatusOrderByCreatedAtAsc(UUID orgId, SubscriptionCreditPurchaseStatus status);
}
