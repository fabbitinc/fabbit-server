package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record BomTreeNodeResponse(
        UUID id,
        String partNumber,
        String name,
        String revision,
        String material,
        String unit,
        String category,
        PartLifecycleState lifecycleState,
        BigDecimal quantity,
        List<BomTreeNodeResponse> children
) {
}
