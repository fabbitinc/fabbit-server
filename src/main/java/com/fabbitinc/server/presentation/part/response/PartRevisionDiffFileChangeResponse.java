package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.application.part.model.PartRevisionDiffChangeType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 변경 응답")
public record PartRevisionDiffFileChangeResponse(
        String itemType,
        String displayName,
        PartRevisionDiffChangeType changeType
) {
}
