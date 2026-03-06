package com.fabbitinc.server.application.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "프로젝트 멤버 목록 응답")
public record ProjectMemberListResponse(
        @Schema(description = "프로젝트 멤버 목록")
        List<ProjectMemberSummaryResponse> items
) {
}
