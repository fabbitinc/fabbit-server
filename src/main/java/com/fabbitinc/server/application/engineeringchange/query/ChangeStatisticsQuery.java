package com.fabbitinc.server.application.engineeringchange.query;

import com.fabbitinc.server.application.engineeringchange.query.condition.ChangeStatisticsCondition;
import com.fabbitinc.server.application.engineeringchange.query.condition.StepProgressCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.ChangeStatisticsResult;
import com.fabbitinc.server.application.engineeringchange.query.result.ChangeStatisticsResult.TopChangedPart;
import com.fabbitinc.server.application.engineeringchange.query.result.StepProgressResult;
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
                SELECT pr.part_id, p.part_number, MIN(pr.name) AS part_name, COUNT(DISTINCT ai.engineering_change_id) AS change_count
                FROM engineering_change_affected_items ai
                JOIN engineering_changes ec ON ec.id = ai.engineering_change_id
                JOIN part_revisions pr ON pr.id = ai.target_id
                JOIN parts p ON p.id = pr.part_id
                WHERE ec.state = 'RELEASED'
                GROUP BY pr.part_id, p.part_number
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

    /**
     * 비종료 상태(RELEASED, CANCELED 제외) EC의 단계별 진행 현황 목록을 조회한다.
     */
    @SuppressWarnings("unchecked")
    public List<StepProgressResult> listStepProgress(StepProgressCondition condition) {
        String sql = """
                SELECT
                    ec.id AS engineering_change_id,
                    (SELECT COUNT(*) FROM engineering_change_step_stages ss WHERE ss.engineering_change_id = ec.id)::int AS total_stages,
                    (SELECT COUNT(*) FROM engineering_change_step_stages ss
                     WHERE ss.engineering_change_id = ec.id
                       AND NOT EXISTS (
                           SELECT 1 FROM engineering_change_steps s
                           WHERE s.step_stage_id = ss.id
                             AND s.status NOT IN ('APPROVED', 'CANCELED')
                       )
                    )::int AS completed_stages,
                    cur.step_type AS current_stage_type,
                    (SELECT COUNT(*) FROM engineering_change_steps s WHERE s.step_stage_id = cur.id)::int AS current_stage_steps_total,
                    (SELECT COUNT(*) FROM engineering_change_steps s WHERE s.step_stage_id = cur.id AND s.status = 'APPROVED')::int AS current_stage_steps_approved,
                    (SELECT COUNT(*) FROM engineering_change_steps s WHERE s.step_stage_id = cur.id AND s.status = 'PENDING')::int AS current_stage_steps_pending,
                    (SELECT COUNT(*) FROM engineering_change_steps s WHERE s.step_stage_id = cur.id AND s.status = 'CHANGES_REQUESTED')::int AS current_stage_steps_changes_requested
                FROM engineering_changes ec
                LEFT JOIN LATERAL (
                    SELECT ss.id, ss.step_type
                    FROM engineering_change_step_stages ss
                    WHERE ss.engineering_change_id = ec.id
                      AND EXISTS (
                          SELECT 1 FROM engineering_change_steps s
                          WHERE s.step_stage_id = ss.id
                            AND s.status NOT IN ('APPROVED', 'CANCELED')
                      )
                    ORDER BY ss.sequence
                    FETCH FIRST 1 ROW ONLY
                ) cur ON TRUE
                WHERE ec.state NOT IN ('RELEASED', 'CANCELED')
                ORDER BY ec.created_at DESC
                """;
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new StepProgressResult(
                        (UUID) row[0],
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue(),
                        (String) row[3],
                        ((Number) row[4]).intValue(),
                        ((Number) row[5]).intValue(),
                        ((Number) row[6]).intValue(),
                        ((Number) row[7]).intValue()
                ))
                .toList();
    }
}
