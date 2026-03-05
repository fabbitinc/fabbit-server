package com.fabbitinc.server.application.file.dto.response;

import com.fabbitinc.server.domain.file.model.FileStatus;

import java.time.Instant;
import java.util.UUID;

public record FileCompleteResponse(
        UUID fileId,
        FileStatus status,
        String originalName,
        String fileKey,
        long fileSize,
        String contentType,
        Instant createdAt
) {
}
