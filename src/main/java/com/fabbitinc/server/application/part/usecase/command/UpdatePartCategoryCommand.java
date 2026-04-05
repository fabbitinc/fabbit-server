package com.fabbitinc.server.application.part.usecase.command;

import com.fabbitinc.server.domain.part.model.PartItemType;
import java.util.UUID;

public record UpdatePartCategoryCommand(
        UUID categoryId,
        String name,
        PartItemType itemType,
        String prefix,
        String delimiter,
        int digits
) {
}
