package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record BomParentResponse(
        UUID partId,
        UUID revisionId,
        String partNumber,
        String name,
        String revisionCode,
        PartRevisionStatus revisionStatus,
        String lineNumber,
        BigDecimal quantity,
        Map<String, Object> extendedProperties
) {
}
