package com.fabbitinc.server.application.synthesis.dto.response;

import java.util.List;
import java.util.UUID;

public record SynthesisBatchStartResponse(
        UUID batchId,
        int requestedCount,
        int acceptedCount,
        List<SynthesisJobResponse> items,
        List<SynthesisBatchFailure> failed
) {
}
