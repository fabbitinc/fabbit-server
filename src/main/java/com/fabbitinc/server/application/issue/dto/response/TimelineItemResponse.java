package com.fabbitinc.server.application.issue.dto.response;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.activity.model.ActivityScope;
import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "타임라인 항목")
public record TimelineItemResponse(
        TimelineItemType type,
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
