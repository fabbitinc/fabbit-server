package com.fabbitinc.server.application.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "부품 연결 프로젝트 요약 정보")
public record PartProjectSummaryResponse(
        @Schema(description = "프로젝트 ID")
        UUID id,
        @Schema(description = "프로젝트 이름", example = "신규 BOM 검토")
        String name,
        @Schema(description = "프로젝트 설명", example = "2026년 1분기 양산 준비 프로젝트")
        String description
) {
}
