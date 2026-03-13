package com.fabbitinc.server.application.part.query.condition;

public record PartSuppliersCondition(
        String partNumber,
        String revisionCode
) {
}
