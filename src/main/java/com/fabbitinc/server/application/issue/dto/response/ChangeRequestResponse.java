package com.fabbitinc.server.application.issue.dto.response;

import tools.jackson.databind.JsonNode;
import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "변경요청 상세 응답")
public record ChangeRequestResponse(
        UUID id,
        int number,
        String type,
        String title,
        JsonNode body,
        String state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean isModified,
        IssueUserSummaryResponse createdBy,
        List<LabelBadgeResponse> labels,
        List<IssueUserSummaryResponse> assignees,
        List<TeamBadgeResponse> assignedTeams,
        List<ReviewerSummaryResponse> reviewers,
        List<TeamBadgeResponse> reviewerTeams,
        List<PartBadgeResponse> parts,
        List<FileItemResponse> files,
        int commentsCount,
        String crState,
        Instant mergedAt,
        UUID mergedBy,
        List<LinkedIssueBadgeResponse> linkedIssues
) {
}
