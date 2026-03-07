package com.fabbitinc.server.application.synthesis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "합성 시작 실패 항목")
public record SynthesisBatchFailure(
        @Schema(description = "실패한 파일 ID")
        UUID fileId,
        @Schema(description = "실패 사유", example = "지원하지 않는 파일 형식입니다")
        String reason
) {
}
