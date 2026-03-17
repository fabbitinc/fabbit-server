package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartPreviewProcessingStatus;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import java.util.UUID;

public record PartPreviewProcessingResult(
        PartPreviewSourceType sourceType,
        UUID sourceId,
        PartPreviewProcessingStatus status,
        PartPreviewProcessingFailureCode failureCode,
        String failureMessage,
        boolean pdfReady,
        boolean webpReady,
        boolean glbReady
) {
}
