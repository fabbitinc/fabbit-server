package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.application.part.model.PartRevisionDiffChangeType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "속성 변경 응답")
public record PartRevisionDiffAttributeChangeResponse(
        String fieldKey,
        String fieldLabel,
        PartRevisionDiffChangeType changeType,
        String beforeValue,
        String afterValue
) {
}
