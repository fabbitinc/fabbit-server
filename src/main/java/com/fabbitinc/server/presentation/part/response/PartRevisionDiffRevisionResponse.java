package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "diff 비교 리비전 응답")
public record PartRevisionDiffRevisionResponse(
        UUID revisionId,
        String revisionCode,
        PartRevisionStatus status,
        Instant createdAt,
        PartOwnerUserSummaryResponse createdBy
) {
}
