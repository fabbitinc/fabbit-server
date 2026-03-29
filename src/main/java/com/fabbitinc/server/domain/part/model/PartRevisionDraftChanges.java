package com.fabbitinc.server.domain.part.model;

public record PartRevisionDraftChanges(
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
        String extendedProperties,
        boolean extendedPropertiesSet
) {
    public boolean hasAnyChange() {
        return nameSet
                || materialSet
                || unitSet
                || descriptionSet
                || leadTimeDaysSet
                || extendedPropertiesSet;
    }
}
