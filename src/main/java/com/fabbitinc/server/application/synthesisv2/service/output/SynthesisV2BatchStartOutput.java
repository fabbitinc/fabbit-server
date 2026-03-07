package com.fabbitinc.server.application.synthesisv2.service.output;

import java.util.List;
import java.util.UUID;

public record SynthesisV2BatchStartOutput(
        UUID batchId,
        int requestedCount,
        int acceptedCount,
        List<SynthesisV2JobOutput> items,
        List<SynthesisV2BatchFailureOutput> failed
) {
}
