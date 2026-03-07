package com.fabbitinc.server.application.mappingv2.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "행 안의 노드 매핑")
public record NodeMappingV2Dto(
        @Schema(description = "행 내부 노드 식별자", example = "part_child")
        String nodeId,
        @Schema(description = "노드 라벨", example = "Part")
        String label,
        @Schema(description = "표준 속성 매핑")
        Map<String, String> propertyColumns,
        @Schema(description = "확장 속성 매핑")
        List<ExtendedPropertyMappingV2Dto> extendedProperties,
        @Schema(description = "매핑 신뢰도(0-100)", example = "95")
        Integer confidence,
        @Schema(description = "매핑 근거", example = "child part columns")
        String reason
) {
    public NodeMappingV2Dto {
        nodeId = nodeId == null ? null : nodeId.trim();
        label = label == null ? null : label.trim();
        propertyColumns = propertyColumns == null ? Map.of() : Map.copyOf(propertyColumns);
        extendedProperties = extendedProperties == null ? List.of() : List.copyOf(extendedProperties);
        confidence = confidence == null ? 0 : confidence;
        reason = reason == null ? "" : reason;
    }
}
