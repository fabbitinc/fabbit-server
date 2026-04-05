package com.fabbitinc.server.application.part.usecase.command;

import com.fabbitinc.server.domain.part.model.PartItemType;

public record CreatePartCategoryCommand(
        String name,
        PartItemType itemType,
        String prefix,
        String delimiter,
        int digits
) {
}
