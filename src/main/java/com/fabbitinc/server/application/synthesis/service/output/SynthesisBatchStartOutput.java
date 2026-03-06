package com.fabbitinc.server.application.synthesis.service.output;

import java.util.List;
import java.util.UUID;

public record SynthesisBatchStartOutput(
        UUID batchId,
        int requestedCount,
        int acceptedCount,
        List<SynthesisJobOutput> items,
        List<SynthesisBatchFailureOutput> failed
) {
}
