package com.fabbitinc.server.application.file.dto.response;

import java.time.Instant;
import java.util.UUID;

public record FileItemResponse(
        UUID fileId,
        String originalName,
        String contentType,
        long fileSize,
        String fileUrl,
        Instant createdAt
) {
}
