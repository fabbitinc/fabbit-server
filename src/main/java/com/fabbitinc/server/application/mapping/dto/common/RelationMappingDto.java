package com.fabbitinc.server.application.mapping.dto.common;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "관계 매핑")
public record RelationMappingDto(
        @Schema(description = "관계 타입", example = "CONSISTS_OF")
        RelationshipType relType,
        @Schema(description = "대상 노드 라벨", example = "Part")
        String targetLabel,
        @Schema(description = "대상 노드 속성 매핑")
        Map<String, String> nodeColumns,
        @Schema(description = "관계 속성 매핑")
        Map<String, String> relColumns,
        @Schema(description = "관계 속성 타입")
        Map<String, PropertyDataType> relColumnTypes,
        @Schema(description = "매핑 신뢰도(0-100)", example = "88")
        Integer confidence,
        @Schema(description = "매핑 근거", example = "quantity + parent part headers")
        String reason
) {
    public RelationMappingDto {
        nodeColumns = nodeColumns == null ? Map.of() : Map.copyOf(nodeColumns);
        relColumns = relColumns == null ? Map.of() : Map.copyOf(relColumns);
        relColumnTypes = relColumnTypes == null ? Map.of() : Map.copyOf(relColumnTypes);
        confidence = confidence == null ? 0 : confidence;
        reason = reason == null ? "" : reason;
    }
}
