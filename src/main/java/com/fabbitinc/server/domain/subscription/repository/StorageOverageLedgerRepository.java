package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.StorageOverageLedger;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageOverageLedgerRepository extends JpaRepository<StorageOverageLedger, UUID> {

    List<StorageOverageLedger> findByStatus(SubscriptionBillingLedgerStatus status);

    List<StorageOverageLedger> findByOrgIdAndStatus(UUID orgId, SubscriptionBillingLedgerStatus status);

    boolean existsBySubscriptionIdAndPeriodStartAndPeriodEnd(UUID subscriptionId, Instant periodStart, Instant periodEnd);
}
