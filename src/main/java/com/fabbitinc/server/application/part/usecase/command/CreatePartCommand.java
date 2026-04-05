package com.fabbitinc.server.application.part.usecase.command;

import com.fabbitinc.server.domain.part.model.PartItemType;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.Map;
import java.util.UUID;

public record CreatePartCommand(
        String partNumber,
        UUID categoryId,
        PartItemType itemType,
        String name,
        String material,
        String unit,
        String description,
        PartLifecycleState lifecycleState,
        Integer leadTimeDays,
        Map<String, Object> extendedProperties,
        String reason
) {
}
