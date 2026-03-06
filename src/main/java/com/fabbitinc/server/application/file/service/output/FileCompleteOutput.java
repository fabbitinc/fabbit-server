package com.fabbitinc.server.application.file.service.output;

import com.fabbitinc.server.domain.file.model.FileStatus;

import java.time.Instant;
import java.util.UUID;

public record FileCompleteOutput(
        UUID fileId,
        FileStatus status,
        String originalName,
        String fileKey,
        long fileSize,
        String contentType,
        Instant createdAt
) {
}
