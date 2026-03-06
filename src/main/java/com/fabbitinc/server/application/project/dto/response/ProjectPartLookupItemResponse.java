package com.fabbitinc.server.application.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "프로젝트 부품 후보 요약 정보")
public record ProjectPartLookupItemResponse(
        @Schema(description = "부품 ID")
        UUID id,
        @Schema(description = "부품 번호", example = "BRKT-001")
        String partNumber,
        @Schema(description = "부품 이름", example = "브라켓")
        String name
) {
}
