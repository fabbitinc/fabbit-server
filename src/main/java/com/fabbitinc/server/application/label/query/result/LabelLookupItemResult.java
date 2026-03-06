package com.fabbitinc.server.application.label.query.result;

import java.util.UUID;

public record LabelLookupItemResult(
        UUID id,
        String name,
        String color
) {
}
