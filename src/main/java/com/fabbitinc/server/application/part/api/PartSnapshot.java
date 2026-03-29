package com.fabbitinc.server.application.part.api;

import com.fabbitinc.server.domain.part.model.PartItemType;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.UUID;

public record PartSnapshot(
        UUID id,
        UUID revisionId,
        String partNumber,
        String name,
        String revisionCode,
        String material,
        String unit,
        String description,
        UUID numberingCategoryId,
        PartLifecycleState lifecycleState,
        PartItemType itemType,
        Integer leadTimeDays,
        String extendedProperties
) {
}
