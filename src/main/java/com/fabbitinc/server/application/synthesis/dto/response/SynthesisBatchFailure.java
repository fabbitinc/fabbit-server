package com.fabbitinc.server.application.synthesis.dto.response;

import java.util.UUID;

public record SynthesisBatchFailure(
        UUID fileId,
        String reason
) {
}
