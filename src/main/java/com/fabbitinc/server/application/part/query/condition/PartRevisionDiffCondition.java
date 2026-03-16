package com.fabbitinc.server.application.part.query.condition;

public record PartRevisionDiffCondition(
        String partNumber,
        String revisionCode,
        String baseRevisionCode
) {
}
