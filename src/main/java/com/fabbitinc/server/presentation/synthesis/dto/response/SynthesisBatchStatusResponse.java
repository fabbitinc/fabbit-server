package com.fabbitinc.server.presentation.synthesis.dto.response;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SynthesisBatchStatusResponse(
        UUID batchId,
        int requestedCount,
        int acceptedCount,
        int failedCount,
        int pendingCount,
        int processingCount,
        int completedCount,
        int failedJobCount,
        Status status,
        List<SynthesisBatchFailureResponse> failed,
        List<SynthesisBatchItemStatusResponse> items,
        Instant createdAt
) {
    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        COMPLETED_WITH_ERRORS
    }

    public record SynthesisBatchFailureResponse(
            UUID fileId,
            String reason
    ) {
    }

    public record SynthesisBatchItemStatusResponse(
            UUID jobId,
            UUID fileId,
            SynthesisJobStatus status,
            int totalRows,
            int processedRows,
            int nodesCreated,
            int relationshipsCreated,
            int errorCount,
            Instant startedAt,
            Instant completedAt
    ) {
    }
}
