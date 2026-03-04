package com.fabbitinc.server.application.mapping.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Schema(description = "매핑 결과")
public record MappingResultDto(
        @Schema(description = "Part 속성 매핑 목록")
        List<PropertyMappingDto> propertyMappings,
        @Schema(description = "관계 매핑 목록")
        List<RelationMappingDto> relationMappings
) {
    public MappingResultDto {
        propertyMappings = propertyMappings == null ? List.of() : List.copyOf(propertyMappings);
        relationMappings = relationMappings == null ? List.of() : List.copyOf(relationMappings);
    }

    public List<String> requiredColumns() {
        Set<String> seen = new LinkedHashSet<>();

        for (PropertyMappingDto propertyMapping : propertyMappings) {
            if (propertyMapping.sourceColumn() == null || propertyMapping.sourceColumn().isBlank()) {
                continue;
            }
            seen.add(propertyMapping.sourceColumn());
        }

        for (RelationMappingDto relationMapping : relationMappings) {
            seen.addAll(relationMapping.nodeColumns().values());
            seen.addAll(relationMapping.relColumns().values());
        }

        return new ArrayList<>(seen);
    }
}
