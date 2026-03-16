package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.application.part.model.PartAttachmentType;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartPreviewSourcesResult(
        long total,
        List<Item> items
) {
    public record Item(
            PartAttachmentType attachmentType,
            PartPreviewSourceType sourceType,
            UUID sourceId,
            UUID fileId,
            UUID drawingId,
            String originalName,
            String contentType,
            long fileSize,
            String fileUrl,
            boolean selected,
            boolean deletable,
            Instant createdAt
    ) {
    }
}
