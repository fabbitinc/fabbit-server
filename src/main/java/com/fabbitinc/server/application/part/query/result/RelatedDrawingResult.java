package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.application.part.model.DrawingViewerType;
import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingStatus;
import java.util.UUID;

public record RelatedDrawingResult(
        UUID id,
        String drawingNumber,
        String name,
        String version,
        DrawingStatus status,
        DrawingConversionStatus conversionStatus,
        DrawingViewerType viewerType,
        String viewerUrl,
        String previewUrl,
        String originalFileUrl
) {
}
