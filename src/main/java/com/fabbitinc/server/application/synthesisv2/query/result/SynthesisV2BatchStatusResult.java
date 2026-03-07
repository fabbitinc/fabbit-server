package com.fabbitinc.server.application.synthesisv2.query.result;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SynthesisV2BatchStatusResult(
        UUID batchId,
        int requestedCount,
        int acceptedCount,
        int failedCount,
        int pendingCount,
        int processingCount,
        int completedCount,
        int failedJobCount,
        Status status,
        List<SynthesisV2BatchFailureResult> failed,
        List<SynthesisV2BatchItemStatusResult> items,
        Instant createdAt
) {
    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        COMPLETED_WITH_ERRORS
    }

    public record SynthesisV2BatchFailureResult(
            UUID fileId,
            String reason
    ) {
    }

    public record SynthesisV2BatchItemStatusResult(
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
