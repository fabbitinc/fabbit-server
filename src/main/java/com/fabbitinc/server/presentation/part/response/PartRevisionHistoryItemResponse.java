package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartRevisionReleaseWorkflowType;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "리비전 이력 항목 응답")
public record PartRevisionHistoryItemResponse(
        UUID revisionId,
        String revisionCode,
        PartRevisionStatus status,
        String name,
        PartRevisionDiffSummaryResponse summary,
        List<PartRevisionHistoryEventResponse> events
) {
}
