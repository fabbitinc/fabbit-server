package com.fabbitinc.server.application.synthesis.service.output;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SynthesisJobOutput(
        UUID id,
        UUID mappingId,
        UUID fileId,
        SynthesisJobStatus status,
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
