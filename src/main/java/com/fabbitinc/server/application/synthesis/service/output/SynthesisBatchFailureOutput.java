package com.fabbitinc.server.application.synthesis.service.output;

import java.util.UUID;

public record SynthesisBatchFailureOutput(
        UUID fileId,
        String reason
) {
}
