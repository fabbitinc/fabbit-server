package com.fabbitinc.server.application.engineeringchange.query;

import com.fabbitinc.server.application.engineeringchange.query.condition.ChangeFeedCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.ChangeFeedResult;
import com.fabbitinc.server.application.engineeringchange.query.result.ChangeFeedResult.ChangeFeedItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 릴리즈된 엔지니어링 변경의 피드를 조회한다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChangeFeedQuery {

    private final EntityManager entityManager;

    /**
     * 릴리즈된 EC 피드 목록을 조회한다.
     * partId가 null이면 전체 릴리즈된 EC를, 그렇지 않으면 해당 파트에 영향을 준 EC만 반환한다.
     */
    public ChangeFeedResult listChangeFeed(ChangeFeedCondition condition) {
        // 1) 릴리즈된 EC 목록 조회
        List<Object[]> ecRows = queryReleasedEcs(condition);
        if (ecRows.isEmpty()) {
            return new ChangeFeedResult(List.of());
        }

        // EC ID 순서 유지를 위해 LinkedHashMap 사용
        Map<UUID, EcRow> ecMap = new LinkedHashMap<>();
        for (Object[] row : ecRows) {
            UUID ecId = (UUID) row[0];
            ecMap.put(ecId, new EcRow(
                    ecId,
                    ((Number) row[1]).intValue(),
                    (String) row[2],
                    row[3] == null ? null : ((Instant) row[3]),
                    (UUID) row[4],
                    (String) row[5],
                    row[6] == null ? null : (UUID) row[6]
            ));
        }

        // 2) 영향받는 파트 번호 조회
        Map<UUID, List<String>> affectedPartNumbersMap = queryAffectedPartNumbers(ecMap.keySet().stream().toList());

        // 3) source issue number 조회
        Map<UUID, Integer> sourceIssueNumberMap = querySourceIssueNumbers(
                ecMap.values().stream()
                        .map(EcRow::sourceIssueId)
                        .filter(id -> id != null)
                        .distinct()
                        .toList()
        );

        // 4) 결과 조립
        List<ChangeFeedItem> items = ecMap.values().stream()
                .map(ec -> {
                    List<String> partNumbers = affectedPartNumbersMap.getOrDefault(ec.ecId(), List.of());
                    Integer issueNumber = ec.sourceIssueId() != null
                            ? sourceIssueNumberMap.get(ec.sourceIssueId())
                            : null;
                    return new ChangeFeedItem(
                            ec.ecId(),
                            ec.ecNumber(),
                            ec.title(),
                            partNumbers,
                            partNumbers.size(),
                            ec.releasedAt(),
                            ec.releasedById(),
                            ec.releasedByName(),
                            issueNumber
                    );
                })
                .toList();

        return new ChangeFeedResult(items);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> queryReleasedEcs(ChangeFeedCondition condition) {
        if (condition.partId() != null) {
            // 특정 파트에 영향을 준 릴리즈된 EC 조회
            String sql = """
                    SELECT DISTINCT ec.id, ec.number, ec.title, ec.released_at,
                           ec.released_by, u.full_name, ec.source_issue_id
                    FROM engineering_changes ec
                    JOIN engineering_change_affected_items ai ON ai.engineering_change_id = ec.id
                    JOIN part_revisions pr ON pr.id = ai.target_id
                    LEFT JOIN users u ON u.id = ec.released_by
                    WHERE ec.state = 'RELEASED'
                      AND pr.part_id = :partId
                    ORDER BY ec.released_at DESC
                    OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                    """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("partId", condition.partId());
            query.setParameter("offset", condition.offset());
            query.setParameter("limit", condition.limit());
            return query.getResultList();
        } else {
            // 전체 릴리즈된 EC 조회
            String sql = """
                    SELECT ec.id, ec.number, ec.title, ec.released_at,
                           ec.released_by, u.full_name, ec.source_issue_id
                    FROM engineering_changes ec
                    LEFT JOIN users u ON u.id = ec.released_by
                    WHERE ec.state = 'RELEASED'
                    ORDER BY ec.released_at DESC
                    OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                    """;
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("offset", condition.offset());
            query.setParameter("limit", condition.limit());
            return query.getResultList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, List<String>> queryAffectedPartNumbers(List<UUID> ecIds) {
        if (ecIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT ai.engineering_change_id, pr.part_number
                FROM engineering_change_affected_items ai
                JOIN part_revisions pr ON pr.id = ai.target_id
                WHERE ai.engineering_change_id IN (:ecIds)
                ORDER BY pr.part_number
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("ecIds", ecIds);

        List<Object[]> rows = query.getResultList();
        Map<UUID, List<String>> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            UUID ecId = (UUID) row[0];
            String partNumber = (String) row[1];
            result.computeIfAbsent(ecId, k -> new ArrayList<>()).add(partNumber);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, Integer> querySourceIssueNumbers(List<UUID> issueIds) {
        if (issueIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT i.id, i.number
                FROM issues i
                WHERE i.id IN (:issueIds)
                """;
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("issueIds", issueIds);

        List<Object[]> rows = query.getResultList();
        Map<UUID, Integer> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    private record EcRow(
            UUID ecId,
            int ecNumber,
            String title,
            Instant releasedAt,
            UUID releasedById,
            String releasedByName,
            UUID sourceIssueId
    ) {
    }
}
