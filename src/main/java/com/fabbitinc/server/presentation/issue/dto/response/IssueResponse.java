package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Schema(description = "이슈 상세 응답")
public record IssueResponse(
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
        IssueUserSummaryResponse createdBy,
        List<LabelBadgeResponse> labels,
        List<IssueUserSummaryResponse> assignees,
        List<TeamBadgeResponse> assignedTeams,
        List<PartBadgeResponse> parts,
        List<FileItemResponse> files,
        int commentsCount,
        List<LinkedChangeRequestBadgeResponse> linkedChanges
) {
}
