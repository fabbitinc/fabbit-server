package com.fabbitinc.server.application.workitem.query.result;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.activity.model.ActivityScope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record TimelineResult(
        List<Item> items,
        Map<String, UserSummaryResult> users
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
