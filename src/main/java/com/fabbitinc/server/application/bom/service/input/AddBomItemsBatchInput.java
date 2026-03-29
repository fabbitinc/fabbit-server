package com.fabbitinc.server.application.bom.service.input;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AddBomItemsBatchInput(
        UUID partId,
        UUID revisionId,
        List<Item> items
) {

    public record Item(
            UUID childPartRevisionId,
            String lineNumber,
            BigDecimal quantity,
            Map<String, Object> extendedProperties
    ) {
    }
}
