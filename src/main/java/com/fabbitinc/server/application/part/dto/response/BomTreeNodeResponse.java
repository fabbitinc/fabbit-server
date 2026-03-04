package com.fabbitinc.server.application.part.dto.response;

import java.util.List;
import java.util.UUID;

public record BomTreeNodeResponse(
        UUID id,
        String partNumber,
        String name,
        String revision,
        String material,
        String unit,
        String category,
        String lifecycleState,
        int quantity,
        List<BomTreeNodeResponse> children
) {
}
