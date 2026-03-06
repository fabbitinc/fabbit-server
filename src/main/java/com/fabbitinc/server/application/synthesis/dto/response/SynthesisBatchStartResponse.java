package com.fabbitinc.server.application.synthesis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "합성 시작 응답")
public record SynthesisBatchStartResponse(
        @Schema(description = "합성 배치 ID")
        UUID batchId,
        @Schema(description = "요청 파일 수", example = "3")
        int requestedCount,
        @Schema(description = "수락된 파일 수", example = "2")
        int acceptedCount,
        @Schema(description = "생성된 합성 작업 목록")
        List<SynthesisJobResponse> items,
        @Schema(description = "시작 실패 항목 목록")
        List<SynthesisBatchFailure> failed
) {
}
