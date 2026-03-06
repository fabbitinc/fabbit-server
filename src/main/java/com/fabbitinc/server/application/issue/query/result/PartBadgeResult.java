package com.fabbitinc.server.application.issue.query.result;

import java.util.UUID;

public record PartBadgeResult(
        UUID id,
        String partNumber,
        String name
) {
}
