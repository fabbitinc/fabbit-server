package com.fabbitinc.server.application.team.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank(message = "name은 필수입니다")
        @Size(min = 1, max = 100, message = "name은 1~100자여야 합니다")
        String name,

        String description
) {
}
