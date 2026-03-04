package com.fabbitinc.server.application.label.dto.response;

import java.util.UUID;

public record LabelLookupItemResponse(
        UUID id,
        String name,
        String color
) {
}
