package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueType;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IssueDetailResult(
        UUID id,
        int number,
        IssueType type,
        String title,
        JsonNode body,
        IssueState state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean isModified,
        IssueUserSummaryResult createdBy,
        List<LabelBadgeResult> labels,
        List<IssueUserSummaryResult> assignees,
        List<TeamBadgeResult> assignedTeams,
        List<PartBadgeResult> parts,
        List<IssueFileItemResult> files,
        int commentsCount,
        List<LinkedChangeRequestBadgeResult> linkedChanges
) {
}
