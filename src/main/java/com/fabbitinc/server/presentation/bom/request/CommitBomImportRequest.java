package com.fabbitinc.server.presentation.bom.request;

import com.fabbitinc.server.application.bom.usecase.command.CommitBomImportCommand.BomImportMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "BOM 가져오기 확정 요청")
public record CommitBomImportRequest(

        @Schema(description = "업로드된 파일 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "파일 ID는 필수입니다") UUID fileId,

        @Schema(description = "가져오기 모드 (APPEND: 기존 항목에 추가, REPLACE: 기존 항목 대체)", example = "APPEND")
        @NotNull(message = "가져오기 모드는 필수입니다") BomImportMode mode
) {
}
