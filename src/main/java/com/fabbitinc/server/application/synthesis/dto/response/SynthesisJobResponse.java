package com.fabbitinc.server.application.synthesis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "합성 작업 응답")
public record SynthesisJobResponse(
        @Schema(description = "합성 작업 ID")
        UUID id,
        @Schema(description = "매핑 ID")
        UUID mappingId,
        @Schema(description = "원본 파일 ID")
        UUID fileId,
        @Schema(description = "작업 상태", example = "PROCESSING")
        SynthesisJobStatus status,
        @Schema(description = "전체 행 수", example = "100")
        int totalRows,
        @Schema(description = "처리된 행 수", example = "80")
        int processedRows,
        @Schema(description = "생성된 노드 수", example = "120")
        int nodesCreated,
        @Schema(description = "생성된 관계 수", example = "240")
        int relationshipsCreated,
        @Schema(description = "오류 메시지 목록")
        List<String> errors,
        @Schema(description = "시작 시각")
        Instant startedAt,
        @Schema(description = "완료 시각")
        Instant completedAt,
        @Schema(description = "생성 시각")
        Instant createdAt
) {
}
