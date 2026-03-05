package com.fabbitinc.server.application.synthesis.dto.response;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;

import java.time.Instant;
import java.util.UUID;

public record SynthesisBatchItemStatus(
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
