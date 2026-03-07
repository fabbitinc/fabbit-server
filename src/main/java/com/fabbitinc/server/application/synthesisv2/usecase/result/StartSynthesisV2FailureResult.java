package com.fabbitinc.server.application.synthesisv2.usecase.result;

import java.util.UUID;

public record StartSynthesisV2FailureResult(
        UUID fileId,
        String reason
) {
}
