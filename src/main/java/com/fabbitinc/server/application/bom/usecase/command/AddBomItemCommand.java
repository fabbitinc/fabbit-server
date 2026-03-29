package com.fabbitinc.server.application.bom.usecase.command;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record AddBomItemCommand(
        UUID partId,
        UUID revisionId,
        UUID childPartRevisionId,
        String lineNumber,
        BigDecimal quantity,
        Map<String, Object> extendedProperties
) {
}
