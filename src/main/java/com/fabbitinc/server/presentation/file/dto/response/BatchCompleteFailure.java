package com.fabbitinc.server.presentation.file.dto.response;

import java.util.UUID;

public record BatchCompleteFailure(
        UUID fileId,
        String reason
) {
}
