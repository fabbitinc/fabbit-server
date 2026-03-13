package com.fabbitinc.server.application.part.query.condition;

public record PartOwnerCondition(
        String partNumber,
        String revisionCode
) {
}
