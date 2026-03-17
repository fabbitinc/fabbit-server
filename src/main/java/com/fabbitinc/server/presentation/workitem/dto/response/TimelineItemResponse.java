package com.fabbitinc.server.presentation.workitem.dto.response;

import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.activity.model.ActivityScope;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Schema(description = "타임라인 항목")
public record TimelineItemResponse(
        TimelineItemType type,
        UUID id,
        ActivityAction action,
        ActivityScope scope,
        UserSummaryResponse actor,
        TimelineDetailResponse detail,
        JsonNode body,
        UserSummaryResponse author,
        Instant createdAt,
        Instant updatedAt,
        Boolean isModified
) {
}
