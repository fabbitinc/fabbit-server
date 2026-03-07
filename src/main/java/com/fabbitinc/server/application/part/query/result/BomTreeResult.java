package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.application.part.model.BomDirection;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.List;
import java.util.UUID;

public record BomTreeResult(
        Node root,
        BomDirection direction,
        int totalCount
) {
    public record Node(
            UUID id,
            String partNumber,
            String name,
            String revision,
            String material,
            String unit,
            String category,
            PartLifecycleState lifecycleState,
            int quantity,
            List<Node> children
    ) {
    }
}
