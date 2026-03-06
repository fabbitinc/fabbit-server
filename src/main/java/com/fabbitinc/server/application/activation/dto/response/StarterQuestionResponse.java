package com.fabbitinc.server.application.activation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "추천 질문 항목")
public record StarterQuestionResponse(
        @Schema(description = "추천 질문", example = "최근 추가된 Part를 보여줘")
        String question,
        @Schema(description = "질문 설명", example = "최근 변경 흐름을 빠르게 파악할 수 있습니다")
        String description
) {
}
