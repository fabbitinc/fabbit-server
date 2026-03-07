package com.fabbitinc.server.presentation.ontology.dto.response;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "온톨로지 스키마 응답")
public record OntologySchemaResponse(
        @Schema(description = "스키마 이름", example = "fabbit-ontology")
        String name,
        @Schema(description = "스키마 설명")
        String description,
        @Schema(description = "노드 라벨 정의 목록")
        List<NodeLabelSchemaResponse> nodeLabels,
        @Schema(description = "관계 타입 정의 목록")
        List<RelationshipTypeSchemaResponse> relationshipTypes
) {
    @Schema(description = "노드 라벨 정의")
    public record NodeLabelSchemaResponse(
            @Schema(description = "노드 라벨", example = "Part")
            String label,
            @Schema(description = "라벨 설명")
            String description,
            @Schema(description = "속성 정의 목록")
            List<PropertySchemaResponse> properties,
            @Schema(description = "머지 키 목록")
            List<String> mergeKeys
    ) {
    }

    @Schema(description = "관계 타입 정의")
    public record RelationshipTypeSchemaResponse(
            @Schema(description = "관계 타입", example = "HAS_DRAWING")
            RelationshipType relType,
            @Schema(description = "관계 설명")
            String description,
            @Schema(description = "시작 라벨", example = "Part")
            String fromLabel,
            @Schema(description = "종료 라벨", example = "Drawing")
            String toLabel,
            @Schema(description = "속성 정의 목록")
            List<PropertySchemaResponse> properties
    ) {
    }

    @Schema(description = "속성 정의")
    public record PropertySchemaResponse(
            @Schema(description = "속성명", example = "name")
            String name,
            @Schema(description = "속성 설명")
            String description,
            @Schema(description = "데이터 타입", example = "STRING")
            PropertyDataType dataType,
            @Schema(description = "필수 여부", example = "true")
            boolean required,
            @Schema(description = "머지 키 여부", example = "false")
            boolean isMergeKey
    ) {
    }
}
