package com.fabbitinc.server.application.issue.dto.response;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "타임라인 항목")
public record TimelineItemResponse(
        String type,
        UUID id,
        String action,
        String scope,
        UUID actorId,
        JsonNode detail,
        JsonNode body,
        UUID authorId,
        Instant createdAt,
        Instant updatedAt,
        Boolean isModified
) {
}
