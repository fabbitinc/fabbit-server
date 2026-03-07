package com.fabbitinc.server.application.dashboard.dto.response;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "최근 합성 작업 정보")
public record LastSynthesisResponse(
        @Schema(description = "합성 작업 ID")
        UUID jobId,
        @Schema(description = "합성 상태", example = "COMPLETED")
        SynthesisJobStatus status,
        @Schema(description = "완료 시각")
        Instant completedAt,
        @Schema(description = "생성된 노드 수", example = "120")
        int nodesCreated,
        @Schema(description = "생성된 관계 수", example = "240")
        int relationshipsCreated
) {
}
