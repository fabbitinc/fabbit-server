package com.fabbitinc.server.application.workitem.query.result;

import java.time.Instant;
import java.util.UUID;

public record FileItemResult(
        UUID fileId,
        String originalName,
        String contentType,
        long fileSize,
        String fileUrl,
        Instant createdAt
) {
}
