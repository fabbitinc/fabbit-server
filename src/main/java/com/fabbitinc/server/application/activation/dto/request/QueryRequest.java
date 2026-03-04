package com.fabbitinc.server.application.activation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record QueryRequest(
        @NotBlank(message = "question은 비어 있을 수 없습니다")
        String question
) {
}
