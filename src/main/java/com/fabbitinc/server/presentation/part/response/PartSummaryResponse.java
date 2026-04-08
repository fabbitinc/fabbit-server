package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record PartSummaryResponse(
        UUID id,
        UUID revisionId,
        String partNumber,
        String name,
        String category,
        String revisionCode,
        PartRevisionStatus revisionStatus,
        PartLifecycleState lifecycleState,
        boolean hasDrawing,
        long childrenCount,
        boolean hasStaleChildReference
) {
}
