package com.fabbitinc.server.application.issue.usecase.result;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record CommentResult(
        UUID id,
        UUID issueId,
        JsonNode body,
        Instant createdAt,
        Instant updatedAt,
        boolean isModified,
        UUID createdBy
) {
}
