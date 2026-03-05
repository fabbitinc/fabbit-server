package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;

import java.util.UUID;

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
