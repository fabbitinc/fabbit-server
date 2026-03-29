package com.fabbitinc.server.application.bom.usecase.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AddBomItemsBatchCommand(
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
