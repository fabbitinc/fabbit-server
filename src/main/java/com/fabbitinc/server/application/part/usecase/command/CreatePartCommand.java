package com.fabbitinc.server.application.part.usecase.command;

import java.util.Map;

public record CreatePartCommand(
        String partNumber,
        String name,
        String material,
        String unit,
        String description,
        String category,
        Boolean isPhantom,
        String lifecycleState,
        Integer leadTimeDays,
        Map<String, Object> extendedProperties
) {
}
