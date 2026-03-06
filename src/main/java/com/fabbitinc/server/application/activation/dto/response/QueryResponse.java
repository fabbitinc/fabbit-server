package com.fabbitinc.server.application.activation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "그래프 질의 응답")
public record QueryResponse(
        @Schema(description = "질의 결과 목록")
        List<QueryResultResponse> results,
        @Schema(description = "질의 결과를 요약한 답변", example = "최근 생성된 프로젝트는 5건입니다")
        String answer
) {
}
