package com.fabbitinc.server.application.project.query.result;

import java.util.UUID;

public record ProjectPartLookupItemResult(
        UUID id,
        String partNumber,
        String name
) {
}
