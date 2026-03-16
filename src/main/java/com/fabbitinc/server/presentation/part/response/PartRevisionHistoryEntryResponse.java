package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartRevisionHistoryActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "리비전 이력 엔트리 응답")
public record PartRevisionHistoryEntryResponse(
        PartRevisionHistoryActionType actionType,
        Instant occurredAt,
        PartUserSummaryResponse actor,
        String reason
) {
}
