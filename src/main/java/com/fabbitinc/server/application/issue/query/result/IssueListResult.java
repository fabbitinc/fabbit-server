package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.workitem.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.domain.issue.model.IssueState;
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
            String title,
            IssueState state,
            Instant closedAt,
            Instant createdAt,
            Instant updatedAt,
            UserSummaryResult createdBy,
            List<LabelBadgeResult> labels,
            List<UserSummaryResult> assignees,
            List<TeamBadgeResult> assignedTeams,
            List<PartBadgeResult> parts,
            List<FileItemResult> files,
            int commentsCount
    ) {
    }
}
