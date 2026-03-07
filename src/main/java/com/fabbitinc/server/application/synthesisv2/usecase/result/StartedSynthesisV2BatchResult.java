package com.fabbitinc.server.application.synthesisv2.usecase.result;

import java.util.List;
import java.util.UUID;

public record StartedSynthesisV2BatchResult(
        UUID batchId,
        int requestedCount,
        int acceptedCount,
        List<StartedSynthesisV2JobResult> items,
        List<StartSynthesisV2FailureResult> failed
) {
}
