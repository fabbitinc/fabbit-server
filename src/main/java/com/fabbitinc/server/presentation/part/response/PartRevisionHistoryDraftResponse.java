package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.application.part.query.result.PartRevisionCreationSourceType;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "공식 리비전에서 파생된 초안 이력 응답")
public record PartRevisionHistoryDraftResponse(
        UUID revisionId,
        String name,
        PartRevisionStatus status,
        Instant createdAt,
        PartUserSummaryResponse createdBy,
        PartRevisionCreationSourceType creationSourceType,
        Instant completedAt,
        PartUserSummaryResponse completedBy,
        String releasedRevisionCode,
        String reason
) {
}
