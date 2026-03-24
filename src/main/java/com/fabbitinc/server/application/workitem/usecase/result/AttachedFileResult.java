package com.fabbitinc.server.application.workitem.usecase.result;
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
