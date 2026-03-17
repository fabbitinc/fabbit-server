package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record PartSuppliersCondition(
        UUID partId,
        UUID revisionId
) {
}
