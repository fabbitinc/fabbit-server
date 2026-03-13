package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.application.part.model.DrawingViewerType;
import com.fabbitinc.server.domain.part.model.PartPreviewProcessingStatus;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import java.util.UUID;

public record PartPreviewResult(
        UUID id,
        PartPreviewSourceType sourceType,
        UUID sourceId,
        PartPreviewProcessingStatus processingStatus,
        DrawingViewerType viewerType,
        String viewerUrl,
        String previewUrl,
        String originalFileUrl
) {
}
