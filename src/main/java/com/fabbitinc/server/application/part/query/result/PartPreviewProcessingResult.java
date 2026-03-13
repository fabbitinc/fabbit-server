package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartPreviewProcessingStatus;

public record PartPreviewProcessingResult(
        PartPreviewProcessingStatus status,
        PartPreviewProcessingFailureCode failureCode,
        String failureMessage,
        boolean pdfReady,
        boolean webpReady,
        boolean glbReady
) {
}
