package com.fabbitinc.server.application.part.dto.response;

import java.util.UUID;

public record PartSummaryResponse(
        UUID id,
        String partNumber,
        String name,
        String category,
        String revision,
        String lifecycleState,
        String drawingNumber,
        long childrenCount
) {
}
