package com.fabbitinc.server.application.file.usecase.result;

import java.util.UUID;

public record BatchCompleteFailureResult(
        UUID fileId,
        String reason
) {
}
