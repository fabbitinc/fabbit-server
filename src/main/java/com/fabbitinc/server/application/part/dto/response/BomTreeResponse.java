package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.application.part.model.BomDirection;

public record BomTreeResponse(
        BomTreeNodeResponse root,
        BomDirection direction,
        int totalCount
) {
}
