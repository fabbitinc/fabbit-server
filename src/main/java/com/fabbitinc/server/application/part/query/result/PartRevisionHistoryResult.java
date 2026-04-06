package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartRevisionHistoryResult(
        List<Card> items
) {

    public record Card(
            UUID revisionId,
            String revisionCode,
            PartRevisionStatus status,
            String name,
            Instant releasedAt,
            PartUserSummaryResult releasedBy,
            String releaseReason,
            PartRevisionReleaseWorkflowType releaseWorkflowType,
            UUID releaseSourceId,
            Integer releaseSourceNumber,
            String releaseSourceTitle,
            PartRevisionDiffSummaryResult summary,
            List<Draft> drafts
    ) {
    }

    public record Draft(
            UUID revisionId,
            String name,
            PartRevisionStatus status,
            Instant createdAt,
            PartUserSummaryResult createdBy,
            PartRevisionCreationSourceType creationSourceType,
            Instant completedAt,
            PartUserSummaryResult completedBy,
            String releasedRevisionCode,
            String reason
    ) {
    }
}
