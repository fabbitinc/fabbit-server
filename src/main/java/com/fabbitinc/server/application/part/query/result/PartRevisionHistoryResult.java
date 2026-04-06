package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionCreationSourceType;
import com.fabbitinc.server.domain.part.model.PartRevisionReleaseWorkflowType;
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
            PartRevisionDiffSummaryResult summary,
            List<Event> events
    ) {
    }

    public record Event(
            PartRevisionHistoryEventType eventType,
            Instant occurredAt,
            PartUserSummaryResult actor,
            String reason,
            PartRevisionCreationSourceType creationSourceType,
            PartRevisionReleaseWorkflowType releaseWorkflowType,
            UUID draftRevisionId,
            UUID targetRevisionId,
            String targetRevisionCode,
            UUID sourceRefId,
            Integer sourceRefNumber,
            String sourceRefTitle
    ) {
    }
}
