package com.fabbitinc.server.application.mapping.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Schema(description = "매핑 결과")
public record MappingResultDto(
        @Schema(description = "행 안의 노드 목록")
        List<NodeMappingDto> nodes,
        @Schema(description = "노드 간 관계 목록")
        List<RelationMappingDto> relations
) {
    public MappingResultDto {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        relations = relations == null ? List.of() : List.copyOf(relations);
    }

    public List<String> requiredColumns() {
        Set<String> seen = new LinkedHashSet<>();

        for (NodeMappingDto node : nodes) {
            seen.addAll(node.propertyColumns().values());
            node.extendedProperties().stream()
                    .map(ExtendedPropertyMappingDto::sourceColumn)
                    .forEach(seen::add);
        }

        for (RelationMappingDto relation : relations) {
            seen.addAll(relation.propertyColumns().values());
            relation.extendedProperties().stream()
                    .map(ExtendedPropertyMappingDto::sourceColumn)
                    .forEach(seen::add);
        }

        seen.removeIf(column -> column == null || column.isBlank());
        return new ArrayList<>(seen);
    }
}
