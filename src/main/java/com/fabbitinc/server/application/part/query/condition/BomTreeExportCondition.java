package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record BomTreeExportCondition(
        UUID partId,
        UUID revisionId,
        String direction,
        UUID mappingId
) {
}
