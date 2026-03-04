package com.fabbitinc.server.application.dashboard.dto.response;

import java.time.Instant;
import java.util.UUID;

public record LastSynthesisResponse(
        UUID jobId,
        String status,
        Instant completedAt,
        int nodesCreated,
        int relationshipsCreated
) {
}
