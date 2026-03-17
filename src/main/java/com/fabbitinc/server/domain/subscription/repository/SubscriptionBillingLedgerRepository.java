package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedger;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerStatus;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionBillingLedgerRepository extends JpaRepository<SubscriptionBillingLedger, UUID> {

    List<SubscriptionBillingLedger> findByStatusOrderByCreatedAtAsc(SubscriptionBillingLedgerStatus status);

    List<SubscriptionBillingLedger> findByOrgIdAndStatus(UUID orgId, SubscriptionBillingLedgerStatus status);

    List<SubscriptionBillingLedger> findBySubscriptionIdAndLedgerType(UUID subscriptionId, SubscriptionBillingLedgerType ledgerType);

    boolean existsBySubscriptionIdAndLedgerTypeAndPeriodStartAndPeriodEnd(
            UUID subscriptionId,
            SubscriptionBillingLedgerType ledgerType,
            Instant periodStart,
            Instant periodEnd
    );

    boolean existsBySubscriptionIdAndLedgerTypeAndPeriodStartAndPeriodEndAndReferenceType(
            UUID subscriptionId,
            SubscriptionBillingLedgerType ledgerType,
            Instant periodStart,
            Instant periodEnd,
            String referenceType
    );
}
