package com.fabbitinc.server.application.file.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.domain.file.model.FileStatus;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "응답 DTO")
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
