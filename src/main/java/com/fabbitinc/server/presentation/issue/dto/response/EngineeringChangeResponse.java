package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.domain.issue.model.EngineeringChangeState;
import com.fabbitinc.server.domain.issue.model.IssueState;
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
        IssueState state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean isModified,
        IssueUserSummaryResponse createdBy,
        List<ReviewerSummaryResponse> reviewers,
        List<TeamBadgeResponse> reviewerTeams,
        List<EngineeringChangePartRevisionResponse> partRevisions,
        List<FileItemResponse> files,
        int commentsCount,
        EngineeringChangeState engineeringChangeState,
        Instant mergedAt,
        UUID mergedBy,
        List<LinkedIssueBadgeResponse> linkedIssues
) {
}
