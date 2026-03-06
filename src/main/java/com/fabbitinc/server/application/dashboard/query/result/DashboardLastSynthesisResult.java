package com.fabbitinc.server.application.dashboard.query.result;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJobStatus;

import java.time.Instant;
import java.util.UUID;

public record DashboardLastSynthesisResult(
        UUID jobId,
        SynthesisJobStatus status,
        Instant completedAt,
        int nodesCreated,
        int relationshipsCreated
) {
}
