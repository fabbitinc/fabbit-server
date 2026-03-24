package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PartBomResult(
        List<Child> children,
        List<Parent> parents
) {
    public record Child(
            UUID partId,
            UUID revisionId,
            String partNumber,
            String name,
            String revisionCode,
            PartRevisionStatus revisionStatus,
            String lineNumber,
            BigDecimal quantity,
            Map<String, Object> extendedProperties
    ) {
    }

    public record Parent(
            UUID partId,
            UUID revisionId,
            String partNumber,
            String name,
            String revisionCode,
            PartRevisionStatus revisionStatus,
            String lineNumber,
            BigDecimal quantity,
            Map<String, Object> extendedProperties
    ) {
    }
}
