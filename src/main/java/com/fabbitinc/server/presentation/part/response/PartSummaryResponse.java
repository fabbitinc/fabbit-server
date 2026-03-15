package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record PartSummaryResponse(
        UUID id,
        String partNumber,
        String name,
        String category,
        String revisionCode,
        PartLifecycleState lifecycleState,
        boolean hasDrawing,
        long childrenCount
) {
}
