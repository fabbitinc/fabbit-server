package com.fabbitinc.server.application.part.usecase.command;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.Map;

public record CreatePartCommand(
        String partNumber,
        String name,
        String material,
        String unit,
        String description,
        String category,
        Boolean isPhantom,
        PartLifecycleState lifecycleState,
        Integer leadTimeDays,
        Map<String, Object> extendedProperties,
        String reason
) {
}
