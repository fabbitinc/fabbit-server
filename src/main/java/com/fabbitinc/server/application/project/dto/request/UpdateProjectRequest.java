package com.fabbitinc.server.application.project.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(min = 1, max = 200, message = "name은 1~200자여야 합니다")
        String name,
        String description
) {
}
