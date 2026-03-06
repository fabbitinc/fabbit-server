package com.fabbitinc.server.application.activation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "추천 질문 목록 응답")
public record StartersResponse(
        @Schema(description = "추천 질문 목록")
        List<StarterQuestionResponse> starters
) {
}
