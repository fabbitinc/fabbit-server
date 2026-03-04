package com.fabbitinc.server.application.synthesis.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SynthesisBatchItemStatus(
        UUID jobId,
        UUID fileId,
        String status,
        int totalRows,
        int processedRows,
        int nodesCreated,
        int relationshipsCreated,
        int errorCount,
        Instant startedAt,
        Instant completedAt
) {
}
