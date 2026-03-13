package com.fabbitinc.server.application.part.query.condition;

public record BomTreeCondition(
        String partNumber,
        String revisionCode,
        String direction
) {
}
