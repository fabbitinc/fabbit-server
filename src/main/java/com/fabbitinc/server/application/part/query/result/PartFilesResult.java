package com.fabbitinc.server.application.part.query.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartFilesResult(
        long total,
        List<Item> items
) {
    public record Item(
            UUID fileId,
            String originalName,
            String contentType,
            long fileSize,
            String fileUrl,
            Instant createdAt
    ) {
    }
}
