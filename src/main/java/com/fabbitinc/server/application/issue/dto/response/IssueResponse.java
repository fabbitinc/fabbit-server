package com.fabbitinc.server.application.issue.dto.response;

import tools.jackson.databind.JsonNode;
import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "이슈 상세 응답")
public record IssueResponse(
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
        List<PartBadgeResponse> parts,
        List<FileItemResponse> files,
        int commentsCount,
        List<LinkedChangeRequestBadgeResponse> linkedChanges
) {
}
