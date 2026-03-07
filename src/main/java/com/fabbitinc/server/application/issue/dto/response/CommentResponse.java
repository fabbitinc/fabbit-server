package com.fabbitinc.server.application.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Schema(description = "댓글 응답")
public record CommentResponse(
        UUID id,
        UUID issueId,
        JsonNode body,
        Instant createdAt,
        Instant updatedAt,
        boolean isModified,
        UUID createdBy
) {
}
