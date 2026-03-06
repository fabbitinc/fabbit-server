package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;

import java.util.UUID;

@Schema(description = "응답 DTO")
public record PartSummaryResponse(
        UUID id,
        String partNumber,
        String name,
        String category,
        String revision,
        PartLifecycleState lifecycleState,
        String drawingNumber,
        long childrenCount
) {
}
