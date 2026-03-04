package com.fabbitinc.server.application.issue.dto.request;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "댓글 수정 요청")
public record UpdateCommentRequest(
        @NotNull
        @Schema(description = "댓글 본문(TipTap JSON)")
        JsonNode body
) {
}
