package com.fabbitinc.server.domain.aiusage.repository;

import com.fabbitinc.server.domain.aiusage.model.AiUsageEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent, UUID> {

    @Query("""
            select coalesce(sum(e.creditsUsed), 0)
            from AiUsageEvent e
            where e.orgId = :orgId
              and e.createdAt >= :periodStart
            """)
    BigDecimal sumCreditsUsed(UUID orgId, Instant periodStart);

    @Query("""
            select coalesce(sum(e.creditsUsed), 0)
            from AiUsageEvent e
            where e.orgId = :orgId
              and e.createdAt >= :periodStart
              and e.createdAt < :periodEnd
              and e.billingStatus = :billingStatus
            """)
    BigDecimal sumCreditsUsed(UUID orgId, Instant periodStart, Instant periodEnd, String billingStatus);

    @Query("""
            select coalesce(sum(e.billableAmount), 0)
            from AiUsageEvent e
            where e.orgId = :orgId
              and e.createdAt >= :periodStart
              and e.createdAt < :periodEnd
              and e.billingStatus = :billingStatus
            """)
    BigDecimal sumBillableAmount(UUID orgId, Instant periodStart, Instant periodEnd, String billingStatus);

    @Query("""
            select e.category, coalesce(sum(e.creditsUsed), 0), count(e.id)
            from AiUsageEvent e
            where e.orgId = :orgId
              and e.createdAt >= :periodStart
            group by e.category
            """)
    List<Object[]> aggregateCreditsByCategory(UUID orgId, Instant periodStart);

    List<AiUsageEvent> findByOrgIdAndBillingStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID orgId,
            String billingStatus,
            Instant periodStart,
            Instant periodEnd
    );
}
