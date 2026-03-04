package com.fabbitinc.server.application.file.dto.response;

import java.time.Instant;
import java.util.UUID;

public record FileCompleteResponse(
        UUID fileId,
        String status,
        String originalName,
        String fileKey,
        long fileSize,
        String contentType,
        Instant createdAt
) {
}
