package com.fabbitinc.server.application.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 멤버 관리 결과 응답")
public record ManageMembersResponse(
        @Schema(description = "처리된 멤버 수", example = "3")
        int count
) {
}
