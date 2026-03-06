package com.fabbitinc.server.application.issue.usecase.result;

import java.time.Instant;
import java.util.UUID;

public record AttachedFileResult(
        UUID fileId,
        String originalName,
        String contentType,
        long fileSize,
        String fileUrl,
        Instant createdAt
) {
}
