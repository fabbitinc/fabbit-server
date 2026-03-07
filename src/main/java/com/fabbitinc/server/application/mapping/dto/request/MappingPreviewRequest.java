package com.fabbitinc.server.application.mapping.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "매핑 미리보기 요청")
public record MappingPreviewRequest(
        @NotNull @Schema(description = "업로드 완료된 파일 ID")
        UUID fileId,
        @Schema(description = "Excel 시트명 (미지정 시 전체 시트 시도)")
        String sheetName
) {
}
