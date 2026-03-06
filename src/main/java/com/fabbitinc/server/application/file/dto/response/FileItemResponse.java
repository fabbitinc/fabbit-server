package com.fabbitinc.server.application.file.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record FileItemResponse(
        UUID fileId,
        String originalName,
        String contentType,
        long fileSize,
        String fileUrl,
        Instant createdAt
) {
}
