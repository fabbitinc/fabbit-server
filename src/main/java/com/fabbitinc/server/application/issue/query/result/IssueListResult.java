package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IssueListResult(
        long openCount,
        long closedCount,
        long total,
        int offset,
        int limit,
        List<Item> items
) {
    public record Item(
            UUID id,
            int number,
            IssueType type,
            String title,
            IssueState state,
            Instant closedAt,
            Instant createdAt,
            Instant updatedAt,
            IssueUserSummaryResult createdBy,
            List<LabelBadgeResult> labels,
            List<IssueUserSummaryResult> assignees,
            List<TeamBadgeResult> assignedTeams,
            List<PartBadgeResult> parts,
            List<IssueFileItemResult> files,
            int commentsCount
    ) {
    }
}
