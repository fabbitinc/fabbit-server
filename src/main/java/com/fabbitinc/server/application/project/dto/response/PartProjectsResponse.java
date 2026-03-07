package com.fabbitinc.server.application.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "부품에 연결된 프로젝트 목록 응답")
public record PartProjectsResponse(
        @Schema(description = "전체 프로젝트 수", example = "4")
        long total,
        @Schema(description = "연결된 프로젝트 목록")
        List<PartProjectSummaryResponse> items
) {
}
