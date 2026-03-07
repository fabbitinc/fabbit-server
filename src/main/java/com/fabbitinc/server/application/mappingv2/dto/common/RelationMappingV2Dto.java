package com.fabbitinc.server.application.mappingv2.dto.common;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "노드 간 관계 매핑")
public record RelationMappingV2Dto(
        @Schema(description = "시작 노드 식별자", example = "part_parent")
        String fromNodeId,
        @Schema(description = "관계 타입", example = "CONSISTS_OF")
        RelationshipType relType,
        @Schema(description = "도착 노드 식별자", example = "part_child")
        String toNodeId,
        @Schema(description = "표준 관계 속성 매핑")
        Map<String, String> propertyColumns,
        @Schema(description = "표준 관계 속성 타입")
        Map<String, PropertyDataType> propertyColumnTypes,
        @Schema(description = "관계 확장 속성 매핑")
        List<ExtendedPropertyMappingV2Dto> extendedProperties,
        @Schema(description = "매핑 신뢰도(0-100)", example = "88")
        Integer confidence,
        @Schema(description = "매핑 근거", example = "parent child relationship")
        String reason
) {
    public RelationMappingV2Dto {
        fromNodeId = fromNodeId == null ? null : fromNodeId.trim();
        toNodeId = toNodeId == null ? null : toNodeId.trim();
        propertyColumns = propertyColumns == null ? Map.of() : Map.copyOf(propertyColumns);
        propertyColumnTypes = propertyColumnTypes == null ? Map.of() : Map.copyOf(propertyColumnTypes);
        extendedProperties = extendedProperties == null ? List.of() : List.copyOf(extendedProperties);
        confidence = confidence == null ? 0 : confidence;
        reason = reason == null ? "" : reason;
    }
}
