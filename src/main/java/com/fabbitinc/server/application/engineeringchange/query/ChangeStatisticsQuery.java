package com.fabbitinc.server.application.engineeringchange.query;

import com.fabbitinc.server.application.engineeringchange.query.condition.ChangeStatisticsCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.ChangeStatisticsResult;
import com.fabbitinc.server.application.engineeringchange.query.result.ChangeStatisticsResult.TopChangedPart;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 엔지니어링 변경 통계를 조회한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChangeStatisticsQuery {

    private final EntityManager entityManager;

    /**
     * 조직 전체 변경 통계를 조회한다.
     */
    public ChangeStatisticsResult getStatistics(ChangeStatisticsCondition condition) {
        int totalReleasedCount = queryTotalReleasedCount();
        int monthlyReleasedCount = queryMonthlyReleasedCount();
        Double averageApprovalDays = queryAverageApprovalDays();
        List<TopChangedPart> topChangedParts = queryTopChangedParts();

        return new ChangeStatisticsResult(
                totalReleasedCount,
                monthlyReleasedCount,
                averageApprovalDays,
                topChangedParts
        );
    }

    private int queryTotalReleasedCount() {
        String sql = """
                SELECT COUNT(*)
                FROM engineering_changes
                WHERE state = 'RELEASED'
                """;
        Query query = entityManager.createNativeQuery(sql);
        Number result = (Number) query.getSingleResult();
        return result.intValue();
    }

    private int queryMonthlyReleasedCount() {
        Instant monthStart = YearMonth.now(ZoneOffset.UTC)
                .atDay(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        String sql = """
                SELECT COUNT(*)
                FROM engineering_changes
                WHERE state = 'RELEASED'
                  AND released_at >= :monthStart
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("monthStart", monthStart);
        Number result = (Number) query.getSingleResult();
        return result.intValue();
    }

    private Double queryAverageApprovalDays() {
        String sql = """
                SELECT AVG(EXTRACT(EPOCH FROM (released_at - created_at)) / 86400.0)
                FROM engineering_changes
                WHERE state = 'RELEASED'
                  AND released_at IS NOT NULL
                  AND created_at IS NOT NULL
                """;
        Query query = entityManager.createNativeQuery(sql);
        Object result = query.getSingleResult();
        if (result == null) {
            return null;
        }
        return ((Number) result).doubleValue();
    }

    @SuppressWarnings("unchecked")
    private List<TopChangedPart> queryTopChangedParts() {
        String sql = """
                SELECT pr.part_id, p.part_number, pr.name, COUNT(DISTINCT ai.engineering_change_id) AS change_count
                FROM engineering_change_affected_items ai
                JOIN engineering_changes ec ON ec.id = ai.engineering_change_id
                JOIN part_revisions pr ON pr.id = ai.target_id
                JOIN parts p ON p.id = pr.part_id
                WHERE ec.state = 'RELEASED'
                GROUP BY pr.part_id, p.part_number, pr.name
                ORDER BY change_count DESC
                FETCH FIRST 5 ROWS ONLY
                """;
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new TopChangedPart(
                        (UUID) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).intValue()
                ))
                .toList();
    }
}
