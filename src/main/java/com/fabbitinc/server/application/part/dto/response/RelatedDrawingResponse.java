package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingStatus;

import java.util.UUID;

@Schema(description = "응답 DTO")
public record RelatedDrawingResponse(
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
