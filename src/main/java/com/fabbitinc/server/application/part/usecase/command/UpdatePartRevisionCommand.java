package com.fabbitinc.server.application.part.usecase.command;

import java.util.Map;
import java.util.UUID;

public record UpdatePartRevisionCommand(
        UUID partId,
        UUID revisionId,
        String name,
        boolean nameSet,
        String material,
        boolean materialSet,
        String unit,
        boolean unitSet,
        String description,
        boolean descriptionSet,
        Integer leadTimeDays,
        boolean leadTimeDaysSet,
        Map<String, Object> extendedProperties,
        boolean extendedPropertiesSet
) {
}
