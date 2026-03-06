package com.fabbitinc.server.application.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "요청 DTO")
public record UpdateTeamRequest(
        @Size(min = 1, max = 100, message = "name은 1~100자여야 합니다")
        String name,

        String description
) {
}
