package com.fabbitinc.server.application.issue.query.result;

import java.util.UUID;

public record PartBadgeResult(
        UUID id,
        UUID revisionId,
        String partNumber,
        String name
) {
}
