package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record PartRevisionDiffCondition(
        UUID partId,
        UUID revisionId,
        UUID baseRevisionId
) {
}
