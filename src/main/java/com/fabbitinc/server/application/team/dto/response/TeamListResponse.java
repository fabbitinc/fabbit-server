package com.fabbitinc.server.application.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "응답 DTO")
public record TeamListResponse(
        List<TeamSummaryResponse> items
) {
}
