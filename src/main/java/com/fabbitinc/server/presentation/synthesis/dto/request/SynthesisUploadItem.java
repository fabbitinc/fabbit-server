package com.fabbitinc.server.presentation.synthesis.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

@Schema(description = "합성 업로드 항목")
public record SynthesisUploadItem(
        @Schema(description = "합성 대상 파일 ID")
        @NotNull(message = "file_id는 필수입니다") UUID fileId,
        @Schema(description = "루트 컨텍스트 값")
        Map<String, String> rootContext
) {
}
