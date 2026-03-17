package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.application.part.model.BomDirection;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BomTreeResult(
        Node root,
        BomDirection direction,
        int totalCount
) {
    public record Node(
            UUID partId,
            UUID revisionId,
            String partNumber,
            String name,
            String revisionCode,
            PartRevisionStatus revisionStatus,
            String material,
            String unit,
            String category,
            PartLifecycleState lifecycleState,
            BigDecimal quantity,
            List<Node> children
    ) {
    }
}
