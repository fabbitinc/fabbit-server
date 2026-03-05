package com.fabbitinc.server.application.project.dto.response;

import java.util.UUID;

public record ProjectPartLookupItemResponse(
        UUID id,
        String partNumber,
        String name
) {
}
