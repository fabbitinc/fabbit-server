package com.fabbitinc.server.application.issue.dto.request;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "이슈 생성 요청")
public record CreateIssueRequest(
        @NotBlank
        @Size(max = 500)
        @Schema(description = "이슈 제목")
        String title,
        @Schema(description = "이슈 본문(TipTap JSON)")
        JsonNode body,
        @Schema(description = "연결 부품 ID 목록")
        List<UUID> partIds,
        @Schema(description = "담당자 ID 목록")
        List<UUID> assigneeUserIds,
        @Schema(description = "팀 담당자 ID 목록")
        List<UUID> teamAssigneeIds,
        @Schema(description = "라벨 ID 목록")
        List<UUID> labelIds,
        @Schema(description = "첨부 파일 ID 목록(최대 20)")
        @Size(max = 20)
        List<UUID> fileIds
) {
    public CreateIssueRequest {
        partIds = partIds == null ? List.of() : List.copyOf(partIds);
        assigneeUserIds = assigneeUserIds == null ? List.of() : List.copyOf(assigneeUserIds);
        teamAssigneeIds = teamAssigneeIds == null ? List.of() : List.copyOf(teamAssigneeIds);
        labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
        fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
    }
}
