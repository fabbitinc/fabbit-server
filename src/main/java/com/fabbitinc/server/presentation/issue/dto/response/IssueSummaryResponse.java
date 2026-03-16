package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TeamBadgeResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import com.fabbitinc.server.domain.issue.model.IssueState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "이슈 목록 항목")
public record IssueSummaryResponse(
        UUID id,
        int number,
        String title,
        IssueState state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        UserSummaryResponse createdBy,
        List<LabelBadgeResponse> labels,
        List<UserSummaryResponse> assignees,
        List<TeamBadgeResponse> assignedTeams,
        List<PartBadgeResponse> parts,
        List<FileItemResponse> files,
        int commentsCount
) {
}
