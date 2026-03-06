package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.application.part.model.BomDirection;

@Schema(description = "응답 DTO")
public record BomTreeResponse(
        BomTreeNodeResponse root,
        BomDirection direction,
        int totalCount
) {
}
