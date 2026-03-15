package com.fabbitinc.server.presentation.activation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그래프 질의 결과 항목")
public record QueryResultResponse(
        @Schema(description = "결과 타입", example = "NODE")
        ActivationResultType type,
        @Schema(description = "결과 식별 키", example = "project:123")
        String key,
        @Schema(description = "결과 라벨", example = "프로젝트 A")
        String label,
        @Schema(description = "결과 설명", example = "2026년 3월 생성")
        String description,
        @Schema(description = "보조 수치 값", example = "5")
        Long value
) {
}
