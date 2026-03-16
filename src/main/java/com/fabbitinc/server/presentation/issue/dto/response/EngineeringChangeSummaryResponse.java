package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.domain.issue.model.EngineeringChangeState;
import com.fabbitinc.server.domain.issue.model.IssueState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "변경관리 목록 항목")
public record EngineeringChangeSummaryResponse(
        UUID id,
        int number,
        String title,
        IssueState state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        IssueUserSummaryResponse createdBy,
        List<ReviewerSummaryResponse> reviewers,
        List<TeamBadgeResponse> reviewerTeams,
        List<FileItemResponse> files,
        int commentsCount,
        EngineeringChangeState engineeringChangeState,
        Instant mergedAt,
        UUID mergedBy
) {
}
