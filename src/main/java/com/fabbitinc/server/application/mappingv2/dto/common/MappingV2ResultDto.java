package com.fabbitinc.server.application.mappingv2.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Schema(description = "V2 매핑 결과")
public record MappingV2ResultDto(
        @Schema(description = "행 안의 노드 목록")
        List<NodeMappingV2Dto> nodes,
        @Schema(description = "노드 간 관계 목록")
        List<RelationMappingV2Dto> relations
) {
    public MappingV2ResultDto {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        relations = relations == null ? List.of() : List.copyOf(relations);
    }

    public List<String> requiredColumns() {
        Set<String> seen = new LinkedHashSet<>();

        for (NodeMappingV2Dto node : nodes) {
            seen.addAll(node.propertyColumns().values());
            node.extendedProperties().stream()
                    .map(ExtendedPropertyMappingV2Dto::sourceColumn)
                    .forEach(seen::add);
        }

        for (RelationMappingV2Dto relation : relations) {
            seen.addAll(relation.propertyColumns().values());
            relation.extendedProperties().stream()
                    .map(ExtendedPropertyMappingV2Dto::sourceColumn)
                    .forEach(seen::add);
        }

        seen.removeIf(column -> column == null || column.isBlank());
        return new ArrayList<>(seen);
    }
}
