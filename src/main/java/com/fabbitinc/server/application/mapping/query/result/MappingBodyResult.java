package com.fabbitinc.server.application.mapping.query.result;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record MappingBodyResult(
        List<PropertyMappingResult> propertyMappings,
        List<RelationMappingResult> relationMappings
) {
    public MappingBodyResult {
        propertyMappings = propertyMappings == null ? List.of() : List.copyOf(propertyMappings);
        relationMappings = relationMappings == null ? List.of() : List.copyOf(relationMappings);
    }

    public List<String> requiredColumns() {
        Set<String> seen = new LinkedHashSet<>();

        for (PropertyMappingResult propertyMapping : propertyMappings) {
            if (propertyMapping.sourceColumn() == null || propertyMapping.sourceColumn().isBlank()) {
                continue;
            }
            seen.add(propertyMapping.sourceColumn());
        }

        for (RelationMappingResult relationMapping : relationMappings) {
            seen.addAll(relationMapping.nodeColumns().values());
            seen.addAll(relationMapping.relColumns().values());
        }

        return new ArrayList<>(seen);
    }
}
