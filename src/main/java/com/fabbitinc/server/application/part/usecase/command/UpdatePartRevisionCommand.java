package com.fabbitinc.server.application.part.usecase.command;

import java.util.Map;

public record UpdatePartRevisionCommand(
        String partNumber,
        String baseRevisionCode,
        String draftKey,
        String name,
        boolean nameSet,
        String material,
        boolean materialSet,
        String unit,
        boolean unitSet,
        String description,
        boolean descriptionSet,
        String category,
        boolean categorySet,
        Boolean phantom,
        boolean phantomSet,
        Integer leadTimeDays,
        boolean leadTimeDaysSet,
        Map<String, Object> extendedProperties,
        boolean extendedPropertiesSet
) {
}
