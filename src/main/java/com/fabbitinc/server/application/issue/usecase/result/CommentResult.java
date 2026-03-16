package com.fabbitinc.server.application.issue.usecase.result;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record CommentResult(
        UUID id,
        UUID targetId,
        JsonNode body,
        Instant createdAt,
        Instant updatedAt,
        boolean isModified,
        UUID createdBy
) {
}
