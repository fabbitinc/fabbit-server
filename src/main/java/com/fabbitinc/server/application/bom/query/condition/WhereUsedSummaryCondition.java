package com.fabbitinc.server.application.bom.query.condition;

import java.util.UUID;

public record WhereUsedSummaryCondition(
        UUID partId,
        UUID revisionId
) {
}
