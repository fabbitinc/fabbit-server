package com.fabbitinc.server.application.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "프로젝트 부품 목록 응답")
public record ProjectPartsResponse(
        @Schema(description = "전체 부품 수", example = "18")
        long total,
        @Schema(description = "프로젝트에 연결된 부품 목록")
        List<ProjectPartSummaryResponse> items
) {
}
