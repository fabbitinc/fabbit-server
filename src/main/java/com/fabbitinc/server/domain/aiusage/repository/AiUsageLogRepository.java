package com.fabbitinc.server.domain.aiusage.repository;

import com.fabbitinc.server.domain.aiusage.model.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, UUID> {

    @Query("""
            select coalesce(sum(l.creditsUsed), 0)
            from AiUsageLog l
            where l.orgId = ?1
              and l.createdAt >= ?2
            """)
    BigDecimal sumCreditsByOrgIdFrom(UUID orgId, Instant periodStart);

    @Query("""
            select l.category as category,
                   coalesce(sum(l.creditsUsed), 0) as creditsUsed,
                   count(l) as usageCount
            from AiUsageLog l
            where l.orgId = ?1
              and l.createdAt >= ?2
            group by l.category
            """)
    List<AiUsageCategorySummary> aggregateCreditsByCategoryFrom(UUID orgId, Instant periodStart);
}
