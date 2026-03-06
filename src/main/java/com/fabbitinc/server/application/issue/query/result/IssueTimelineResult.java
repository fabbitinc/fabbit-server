package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.activity.model.ActivityScope;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record IssueTimelineResult(
        List<Item> items,
        Map<String, IssueUserSummaryResult> users
) {
    public record Item(
            TimelineItemTypeResult type,
            UUID id,
            ActivityAction action,
            ActivityScope scope,
            UUID actorId,
            JsonNode detail,
            JsonNode body,
            UUID authorId,
            Instant createdAt,
            Instant updatedAt,
            Boolean isModified
    ) {
    }
}
