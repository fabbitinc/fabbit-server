package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Schema(description = "이슈로부터 설계변경 생성 요청")
public record CreateEcFromIssueRequest(
        @Size(max = 500) @Schema(description = "변경관리 제목 (미입력 시 이슈 제목 사용)")
        String title,
        @Schema(description = "변경관리 본문(TipTap JSON, 미입력 시 영향 분석 요약 자동 생성)")
        JsonNode body,
        @Schema(description = "검토자 사용자 ID 목록")
        List<UUID> reviewerIds,
        @Schema(description = "승인자 사용자 ID 목록")
        List<UUID> approverIds
) {
    public CreateEcFromIssueRequest {
        reviewerIds = reviewerIds == null ? List.of() : List.copyOf(reviewerIds);
        approverIds = approverIds == null ? List.of() : List.copyOf(approverIds);
    }
}
