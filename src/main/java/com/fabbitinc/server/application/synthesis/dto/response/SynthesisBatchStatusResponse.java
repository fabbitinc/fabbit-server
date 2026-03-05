package com.fabbitinc.server.application.synthesis.dto.response;

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
        SynthesisBatchStatus status,
        List<SynthesisBatchFailure> failed,
        List<SynthesisBatchItemStatus> items,
        Instant createdAt
) {
}
