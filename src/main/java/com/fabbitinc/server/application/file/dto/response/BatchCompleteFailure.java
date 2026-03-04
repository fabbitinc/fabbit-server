package com.fabbitinc.server.application.file.dto.response;

import java.util.UUID;

public record BatchCompleteFailure(
        UUID fileId,
        String reason
) {
}
