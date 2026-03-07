package com.fabbitinc.server.application.activation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "그래프 자연어 질의 요청")
public record QueryRequest(
        @Schema(description = "탐색할 자연어 질문", example = "최근에 생성된 프로젝트를 보여줘")
        @NotBlank(message = "question은 비어 있을 수 없습니다") String question
) {
}
