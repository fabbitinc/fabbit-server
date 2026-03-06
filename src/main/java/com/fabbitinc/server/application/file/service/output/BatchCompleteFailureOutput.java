package com.fabbitinc.server.application.file.service.output;

import java.util.UUID;

public record BatchCompleteFailureOutput(
        UUID fileId,
        String reason
) {
}
