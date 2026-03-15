package com.fabbitinc.server.presentation.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Schema(description = "변경요청 생성 요청")
public record CreateChangeRequestRequest(
        @NotBlank @Size(max = 500) @Schema(description = "변경요청 제목")
        String title,
        @Schema(description = "변경요청 본문(TipTap JSON)")
        JsonNode body,
        @Schema(description = "연결할 이슈 번호")
        Integer issueNumber,
        @Schema(description = "연결 부품 ID 목록")
        List<UUID> partIds,
        @Schema(description = "연결할 부품 초안 목록")
        @Valid
        List<ChangeRequestPartRevisionTargetRequest> partRevisions,
        @Schema(description = "담당자 ID 목록")
        List<UUID> assigneeUserIds,
        @Schema(description = "팀 담당자 ID 목록")
        List<UUID> teamAssigneeIds,
        @Schema(description = "라벨 ID 목록")
        List<UUID> labelIds,
        @Schema(description = "첨부 파일 ID 목록(최대 20)")
        @Size(max = 20) List<UUID> fileIds,
        @Schema(description = "검토자 ID 목록")
        List<UUID> reviewerUserIds,
        @Schema(description = "팀 검토자 ID 목록")
        List<UUID> teamReviewerIds
) {
    public CreateChangeRequestRequest {
        partIds = partIds == null ? List.of() : List.copyOf(partIds);
        partRevisions = partRevisions == null ? List.of() : List.copyOf(partRevisions);
        assigneeUserIds = assigneeUserIds == null ? List.of() : List.copyOf(assigneeUserIds);
        teamAssigneeIds = teamAssigneeIds == null ? List.of() : List.copyOf(teamAssigneeIds);
        labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
        fileIds = fileIds == null ? List.of() : List.copyOf(fileIds);
        reviewerUserIds = reviewerUserIds == null ? List.of() : List.copyOf(reviewerUserIds);
        teamReviewerIds = teamReviewerIds == null ? List.of() : List.copyOf(teamReviewerIds);
    }
}
