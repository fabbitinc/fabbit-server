package com.fabbitinc.server.application.engineeringchange.query.result;

import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EngineeringChangeListResult(
        long openCount,
        long progressCount,
        long doneCount,
        long total,
        int offset,
        int limit,
        List<Item> items
) {
    public record Item(
            UUID id,
            int number,
            String title,
            EngineeringChangeState state,
            Instant closedAt,
            Instant createdAt,
            Instant updatedAt,
            UserSummaryResult createdBy,
            List<EngineeringChangeStepResult> steps,
            List<FileItemResult> files,
            int commentsCount,
            Instant releasedAt,
            UserSummaryResult releasedBy
    ) {
    }
}
