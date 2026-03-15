package com.fabbitinc.server.presentation.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

@Schema(description = "이슈/변경요청 수정 요청")
public record UpdateIssueRequest(
        @Size(min = 1, max = 500) @Schema(description = "이슈 제목")
        String title,
        @Schema(description = "이슈 본문(TipTap JSON)")
        JsonNode body
) {
}
