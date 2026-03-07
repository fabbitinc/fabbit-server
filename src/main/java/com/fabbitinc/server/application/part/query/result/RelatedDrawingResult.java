package com.fabbitinc.server.application.part.query.result;

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
        String thumbnailUrl,
        String pdfUrl,
        String originalFileUrl
) {
}
