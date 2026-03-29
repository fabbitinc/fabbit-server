package com.fabbitinc.server.application.bom.query.condition;

import java.util.UUID;

public record BomCompareCondition(
        UUID sourceRevisionId,
        UUID targetRevisionId
) {
}
