package com.fabbitinc.server.application.drawing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "요청 DTO")
public record RegisterDrawingRequest(
        @NotNull(message = "file_id는 필수입니다") UUID fileId
) {
}
