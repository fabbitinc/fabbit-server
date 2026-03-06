package com.fabbitinc.server.presentation.synthesis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "합성 배치 상태 응답")
public record SynthesisBatchStatusResponse(
        @Schema(description = "배치 ID")
        UUID batchId,
        @Schema(description = "요청 파일 수", example = "3")
        int requestedCount,
        @Schema(description = "수락 파일 수", example = "2")
        int acceptedCount,
        @Schema(description = "시작 실패 수", example = "1")
        int failedCount,
        @Schema(description = "대기 중 작업 수", example = "0")
        int pendingCount,
        @Schema(description = "처리 중 작업 수", example = "1")
        int processingCount,
        @Schema(description = "완료 작업 수", example = "1")
        int completedCount,
        @Schema(description = "실패 작업 수", example = "0")
        int failedJobCount,
        @Schema(description = "배치 상태", example = "PROCESSING")
        Status status,
        @Schema(description = "실패 항목 목록")
        List<SynthesisBatchFailureResponse> failed,
        @Schema(description = "배치 작업 항목 목록")
        List<SynthesisBatchItemStatusResponse> items,
        @Schema(description = "배치 생성 시각")
        Instant createdAt
) {
    @Schema(description = "합성 배치 상태")
    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        COMPLETED_WITH_ERRORS
    }

    @Schema(description = "합성 배치 실패 항목")
    public record SynthesisBatchFailureResponse(
            @Schema(description = "실패한 파일 ID")
            UUID fileId,
            @Schema(description = "실패 사유", example = "매핑 헤더 불일치")
            String reason
    ) {
    }

    @Schema(description = "합성 배치 작업 상태 항목")
    public record SynthesisBatchItemStatusResponse(
            @Schema(description = "작업 ID")
            UUID jobId,
            @Schema(description = "원본 파일 ID")
            UUID fileId,
            @Schema(description = "작업 상태", example = "COMPLETED")
            SynthesisJobStatus status,
            @Schema(description = "전체 행 수", example = "100")
            int totalRows,
            @Schema(description = "처리된 행 수", example = "100")
            int processedRows,
            @Schema(description = "생성된 노드 수", example = "120")
            int nodesCreated,
            @Schema(description = "생성된 관계 수", example = "240")
            int relationshipsCreated,
            @Schema(description = "오류 건수", example = "0")
            int errorCount,
            @Schema(description = "시작 시각")
            Instant startedAt,
            @Schema(description = "완료 시각")
            Instant completedAt
    ) {
    }
}
