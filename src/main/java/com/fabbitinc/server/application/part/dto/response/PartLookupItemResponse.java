package com.fabbitinc.server.application.part.dto.response;

import java.util.UUID;

public record PartLookupItemResponse(
        UUID id,
        String partNumber,
        String name
) {
}
