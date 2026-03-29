package com.fabbitinc.server.application.bom.service.input;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record UpdateBomItemInput(
        UUID partId,
        UUID revisionId,
        UUID bomItemId,
        UUID childPartRevisionId,
        boolean childPartRevisionIdSet,
        String lineNumber,
        boolean lineNumberSet,
        BigDecimal quantity,
        boolean quantitySet,
        Map<String, Object> extendedProperties,
        boolean extendedPropertiesSet
) {

    public boolean hasAnyFieldSet() {
        return childPartRevisionIdSet
                || lineNumberSet
                || quantitySet
                || extendedPropertiesSet;
    }
}
