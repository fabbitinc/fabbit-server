package com.fabbitinc.server.application.bom.query.condition;

import java.util.UUID;

public record BomCompareExportCondition(
        UUID sourceRevisionId,
        UUID targetRevisionId
) {
}
