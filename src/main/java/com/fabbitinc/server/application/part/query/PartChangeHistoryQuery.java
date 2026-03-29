package com.fabbitinc.server.application.part.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.part.query.condition.PartChangeHistoryCondition;
import com.fabbitinc.server.application.part.query.result.PartChangeHistoryResult;
import com.fabbitinc.server.application.user.api.UserApi;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartChangeHistoryQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final UserApi userApi;
    private final EntityManager entityManager;

    public PartChangeHistoryResult get(PartChangeHistoryCondition condition) {
        currentAuthProvider.getCurrentAuth();

        String sql = """
                SELECT created_at AS timestamp, 'ISSUE' AS type, i.id AS reference_id, i.number AS reference_number, i.title, i.created_by AS actor_id
                FROM issues i
                JOIN issue_parts ip ON ip.issue_id = i.id
                WHERE ip.part_id = :partId

                UNION ALL

                SELECT ec.released_at AS timestamp, 'EC_RELEASED' AS type, ec.id AS reference_id, ec.number AS reference_number, ec.title, ec.released_by AS actor_id
                FROM engineering_changes ec
                JOIN engineering_change_affected_items ai ON ai.engineering_change_id = ec.id
                LEFT JOIN part_revisions pr ON pr.id = ai.target_id AND ai.item_type = 'REVISION_RELEASE'
                WHERE ec.released_at IS NOT NULL
                  AND (
                        (ai.item_type = 'REVISION_RELEASE' AND pr.part_id = :partId)
                        OR (ai.item_type = 'LIFECYCLE_CHANGE' AND ai.target_id = :partId)
                  )

                UNION ALL

                SELECT prh.created_at AS timestamp, 'REVISION_HISTORY' AS type, pr.id AS reference_id, 0 AS reference_number, pr.name AS title, prh.created_by AS actor_id
                FROM part_revisions pr
                JOIN part_revision_histories prh ON prh.part_revision_id = pr.id
                WHERE pr.part_id = :partId
                ORDER BY timestamp DESC
                OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
                """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("partId", condition.partId());
        query.setParameter("offset", condition.offset());
        query.setParameter("limit", condition.limit());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        Set<UUID> actorIds = rows.stream()
                .map(row -> (UUID) row[5])
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, User> usersById = actorIds.isEmpty()
                ? Map.of()
                : userApi.getUsersByIdsOrdered(new ArrayList<>(actorIds)).stream()
                        .collect(Collectors.toMap(User::getId, java.util.function.Function.identity()));

        List<PartChangeHistoryResult.ChangeHistoryItem> items = rows.stream()
                .map(row -> {
                    UUID actorId = (UUID) row[5];
                    User actor = actorId == null ? null : usersById.get(actorId);
                    return new PartChangeHistoryResult.ChangeHistoryItem(
                            (Instant) row[0],
                            (String) row[1],
                            (UUID) row[2],
                            ((Number) row[3]).intValue(),
                            (String) row[4],
                            actor == null ? null : actor.getFullName()
                    );
                })
                .toList();

        return new PartChangeHistoryResult(items);
    }
}
