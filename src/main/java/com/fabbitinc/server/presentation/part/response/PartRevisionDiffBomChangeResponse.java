package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.application.part.model.PartRevisionDiffChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "BOM 변경 응답")
public record PartRevisionDiffBomChangeResponse(
        String lineNumber,
        String beforePartNumber,
        String beforeName,
        BigDecimal beforeQuantity,
        String afterPartNumber,
        String afterName,
        BigDecimal afterQuantity,
        PartRevisionDiffChangeType changeType
) {
}
