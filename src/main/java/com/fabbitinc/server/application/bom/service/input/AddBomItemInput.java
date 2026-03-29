package com.fabbitinc.server.application.bom.service.input;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record AddBomItemInput(
        UUID partId,
        UUID revisionId,
        UUID childPartRevisionId,
        String lineNumber,
        BigDecimal quantity,
        Map<String, Object> extendedProperties
) {
}
