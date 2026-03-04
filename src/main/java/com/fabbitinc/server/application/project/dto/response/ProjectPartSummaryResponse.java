package com.fabbitinc.server.application.project.dto.response;

import java.util.UUID;

public record ProjectPartSummaryResponse(
        UUID id,
        String partNumber,
        String name
) {
}
