package com.fabbitinc.server.application.engineeringchange.query.result;

import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record EngineeringChangeDetailResult(
        UUID id,
        int number,
        String title,
        JsonNode body,
        EngineeringChangeState state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean isModified,
        UserSummaryResult createdBy,
        LinkedIssueSummaryResult sourceIssue,
        List<LabelBadgeResult> labels,
        List<EngineeringChangeStepResult> steps,
        List<EngineeringChangeAffectedItemResult> affectedItems,
        List<FileItemResult> files,
        int commentsCount,
        Instant releasedAt,
        UserSummaryResult releasedBy,
        List<LinkedIssueSummaryResult> linkedIssues
) {
}
