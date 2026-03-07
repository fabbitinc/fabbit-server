package com.fabbitinc.server.application.synthesisv2.service.output;

import java.util.UUID;

public record SynthesisV2BatchFailureOutput(
        UUID fileId,
        String reason
) {
}
