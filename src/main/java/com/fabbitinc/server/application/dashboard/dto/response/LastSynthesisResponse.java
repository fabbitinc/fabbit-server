package com.fabbitinc.server.application.dashboard.dto.response;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;

import java.time.Instant;
import java.util.UUID;

public record LastSynthesisResponse(
        UUID jobId,
        SynthesisJobStatus status,
        Instant completedAt,
        int nodesCreated,
        int relationshipsCreated
) {
}
