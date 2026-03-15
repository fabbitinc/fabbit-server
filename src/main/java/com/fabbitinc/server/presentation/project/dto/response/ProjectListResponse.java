package com.fabbitinc.server.presentation.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "프로젝트 목록 응답")
public record ProjectListResponse(
        @Schema(description = "전체 프로젝트 수", example = "12")
        long total,
        @Schema(description = "페이지 시작 오프셋", example = "0")
        int offset,
        @Schema(description = "요청 limit", example = "20")
        int limit,
        @Schema(description = "프로젝트 목록")
        List<ProjectSummaryResponse> items
) {
}
