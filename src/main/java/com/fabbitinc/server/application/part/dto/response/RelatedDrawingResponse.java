package com.fabbitinc.server.application.part.dto.response;

import java.util.UUID;

public record RelatedDrawingResponse(
        UUID id,
        String drawingNumber,
        String name,
        String version,
        String status,
        String conversionStatus,
        String thumbnailUrl,
        String pdfUrl,
        String originalFileUrl
) {
}
