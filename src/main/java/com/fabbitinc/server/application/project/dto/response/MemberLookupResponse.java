package com.fabbitinc.server.application.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "프로젝트 멤버 후보 lookup 응답")
public record MemberLookupResponse(
        @Schema(description = "멤버 후보 목록")
        List<ProjectUserSummaryResponse> items
) {
}
