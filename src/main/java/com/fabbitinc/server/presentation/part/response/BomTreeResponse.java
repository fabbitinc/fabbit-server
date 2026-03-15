package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.application.part.model.BomDirection;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "응답 DTO")
public record BomTreeResponse(
        BomTreeNodeResponse root,
        BomDirection direction,
        int totalCount
) {
}
