package com.fabbitinc.server.application.drawing.query.result;

public record DrawingProcessingResult(
        DrawingProcessingStatus status,
        String failureReason,
        boolean pdfReady,
        boolean webpReady,
        boolean glbReady
) {
}
