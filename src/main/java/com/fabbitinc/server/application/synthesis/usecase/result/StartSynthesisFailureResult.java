package com.fabbitinc.server.application.synthesis.usecase.result;

import java.util.UUID;

public record StartSynthesisFailureResult(
        UUID fileId,
        String reason
) {
}
