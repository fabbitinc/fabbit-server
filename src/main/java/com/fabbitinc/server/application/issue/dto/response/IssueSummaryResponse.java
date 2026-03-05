package com.fabbitinc.server.application.issue.dto.response;

import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "이슈 목록 항목")
public record IssueSummaryResponse(
        UUID id,
        int number,
        IssueType type,
        String title,
        IssueState state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        IssueUserSummaryResponse createdBy,
        List<LabelBadgeResponse> labels,
        List<IssueUserSummaryResponse> assignees,
        List<TeamBadgeResponse> assignedTeams,
        List<PartBadgeResponse> parts,
        List<FileItemResponse> files,
        int commentsCount
) {
}
