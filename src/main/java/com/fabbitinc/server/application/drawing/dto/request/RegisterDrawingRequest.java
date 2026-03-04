package com.fabbitinc.server.application.drawing.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterDrawingRequest(
        @NotNull(message = "file_id는 필수입니다")
        UUID fileId
) {
}
