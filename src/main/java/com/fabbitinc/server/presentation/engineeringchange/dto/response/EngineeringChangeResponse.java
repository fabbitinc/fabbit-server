package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Schema(description = "변경관리 상세 응답")
public record EngineeringChangeResponse(
        UUID id,
        int number,
        String title,
        JsonNode body,
        EngineeringChangeState state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean isModified,
        UserSummaryResponse createdBy,
        LinkedIssueSummaryResponse sourceIssue,
        List<EngineeringChangeStepResponse> steps,
        List<EngineeringChangeAffectedItemResponse> affectedItems,
        List<FileItemResponse> files,
        int commentsCount,
        Instant releasedAt,
        UserSummaryResponse releasedBy,
        List<LinkedIssueSummaryResponse> linkedIssues
) {
}
