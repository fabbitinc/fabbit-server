package com.fabbitinc.server.application.part.dto.response;

public record BomTreeResponse(
        BomTreeNodeResponse root,
        String direction,
        int totalCount
) {
}
