package com.fabbitinc.server.presentation.bom.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "BOM 가져오기 미리보기 요청")
public record PreviewBomImportRequest(

        @Schema(description = "업로드된 파일 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "파일 ID는 필수입니다") UUID fileId
) {
}
