package com.fabbitinc.server.application.synthesis.usecase.result;

import java.util.List;
import java.util.UUID;

public record StartedSynthesisBatchResult(
        UUID batchId,
        int requestedCount,
        int acceptedCount,
        List<StartedSynthesisJobResult> items,
        List<StartSynthesisFailureResult> failed
) {
}
