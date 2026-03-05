package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;

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
        PartLifecycleState lifecycleState,
        int quantity,
        List<BomTreeNodeResponse> children
) {
}
