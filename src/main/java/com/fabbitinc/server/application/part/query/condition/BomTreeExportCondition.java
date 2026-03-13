package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record BomTreeExportCondition(
        String partNumber,
        String revisionCode,
        String direction,
        UUID mappingId
) {
}
