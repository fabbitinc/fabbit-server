package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.application.part.model.PartAttachmentType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartFilesResult(
        long total,
        List<Item> items
) {
    public record Item(
            PartAttachmentType attachmentType,
            UUID fileId,
            UUID drawingId,
            String originalName,
            String contentType,
            long fileSize,
            String fileUrl,
            boolean previewSelectable,
            boolean selectedAsPreview,
            Instant createdAt
    ) {
    }
}
