package com.fabbitinc.server.application.workitem.usecase.result;
import com.fabbitinc.server.application.workitem.usecase.result.AttachedFileResult;

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
