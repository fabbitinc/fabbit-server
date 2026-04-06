package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.application.part.query.result.PartRevisionHistoryEventType;
import com.fabbitinc.server.domain.part.model.PartRevisionCreationSourceType;
import com.fabbitinc.server.domain.part.model.PartRevisionReleaseWorkflowType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "리비전 카드 내부의 시간순 이벤트 응답")
public record PartRevisionHistoryEventResponse(
        PartRevisionHistoryEventType eventType,
        Instant occurredAt,
        PartUserSummaryResponse actor,
        String reason,
        PartRevisionCreationSourceType creationSourceType,
        PartRevisionReleaseWorkflowType releaseWorkflowType,
        UUID draftRevisionId,
        UUID targetRevisionId,
        String targetRevisionCode,
        UUID sourceRefId,
        Integer sourceRefNumber,
        String sourceRefTitle
) {
}
