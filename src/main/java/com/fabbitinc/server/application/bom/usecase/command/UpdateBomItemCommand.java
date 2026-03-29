package com.fabbitinc.server.application.bom.usecase.command;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record UpdateBomItemCommand(
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
}
