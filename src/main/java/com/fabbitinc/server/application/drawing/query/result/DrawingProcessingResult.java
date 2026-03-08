package com.fabbitinc.server.application.drawing.query.result;

public record DrawingProcessingResult(
        DrawingProcessingStatus status,
        DrawingProcessingFailureCode failureCode,
        String failureMessage,
        boolean pdfReady,
        boolean webpReady,
        boolean glbReady
) {
}
