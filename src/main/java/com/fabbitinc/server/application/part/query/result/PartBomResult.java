package com.fabbitinc.server.application.part.query.result;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;

public record PartBomResult(
        List<Child> children,
        List<Parent> parents
) {
    public record Child(
            UUID id,
            String partNumber,
            String name,
            String revisionCode,
            String lineNumber,
            BigDecimal quantity,
            Map<String, Object> extendedProperties
    ) {
    }

    public record Parent(
            UUID id,
            String partNumber,
            String name,
            String revisionCode,
            String lineNumber,
            BigDecimal quantity,
            Map<String, Object> extendedProperties
    ) {
    }
}
