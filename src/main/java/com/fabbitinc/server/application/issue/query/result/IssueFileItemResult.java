package com.fabbitinc.server.application.issue.query.result;

import java.time.Instant;
import java.util.UUID;

public record IssueFileItemResult(
        UUID fileId,
        String originalName,
        String contentType,
        long fileSize,
        String fileUrl,
        Instant createdAt
) {
}
