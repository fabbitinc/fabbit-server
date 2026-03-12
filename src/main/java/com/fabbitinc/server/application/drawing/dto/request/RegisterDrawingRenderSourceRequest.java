package com.fabbitinc.server.application.drawing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "도면 render source 등록 요청 DTO")
public record RegisterDrawingRenderSourceRequest(
        @Schema(description = "업로드 완료된 render source 파일 ID")
        @NotNull(message = "file_id는 필수입니다")
        UUID fileId
) {
}
