package com.fabbitinc.server.application.drawing.query.result;

import com.fabbitinc.server.domain.drawing.model.DrawingActionRequiredReason;
import java.util.List;

public record DrawingProcessingResult(
        DrawingProcessingStatus status,
        DrawingProcessingFailureCode failureCode,
        String failureMessage,
        boolean pdfReady,
        boolean webpReady,
        boolean glbReady,
        DrawingActionRequiredReason actionRequiredReason,
        List<String> allowedRenderSourceExtensions
) {
}
