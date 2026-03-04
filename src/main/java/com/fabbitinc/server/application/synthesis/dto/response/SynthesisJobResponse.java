package com.fabbitinc.server.application.synthesis.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SynthesisJobResponse(
        UUID id,
        UUID mappingId,
        UUID fileId,
        String status,
        int totalRows,
        int processedRows,
        int nodesCreated,
        int relationshipsCreated,
        List<String> errors,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
}
