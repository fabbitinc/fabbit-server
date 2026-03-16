package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.issue.model.EngineeringChangeState;
import com.fabbitinc.server.domain.issue.model.IssueState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

public record EngineeringChangeDetailResult(
        UUID id,
        int number,
        String title,
        JsonNode body,
        IssueState state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean isModified,
        IssueUserSummaryResult createdBy,
        List<ReviewerSummaryResult> reviewers,
        List<TeamBadgeResult> reviewerTeams,
        List<EngineeringChangePartRevisionResult> partRevisions,
        List<IssueFileItemResult> files,
        int commentsCount,
        EngineeringChangeState engineeringChangeState,
        Instant mergedAt,
        UUID mergedBy,
        List<LinkedIssueBadgeResult> linkedIssues
) {
}
