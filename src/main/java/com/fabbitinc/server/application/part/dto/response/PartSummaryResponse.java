package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record PartSummaryResponse(
        UUID id,
        String partNumber,
        String name,
        String category,
        String revision,
        PartLifecycleState lifecycleState,
        boolean hasDrawing,
        long childrenCount
) {
}
