package com.fabbitinc.server.application.mapping.dto.common;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.mapping.support.ExtendedPropertySupport;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Part 속성 매핑")
public record PropertyMappingDto(
        @Schema(description = "원본 컬럼명", example = "품번")
        String sourceColumn,
        @Schema(description = "대상 속성명", example = "part_number")
        String targetProperty,
        @Schema(description = "동일 컬럼의 확장 속성 대안 키", example = "_ext_number")
        String suggestedExtendedProperty,
        @Schema(description = "데이터 타입", example = "string")
        PropertyDataType dataType,
        @Schema(description = "매핑 신뢰도(0-100)", example = "95")
        Integer confidence,
        @Schema(description = "매핑 근거", example = "header exact match")
        String reason,
        @Schema(description = "확장 속성 여부", example = "false")
        Boolean isExtended
) {
    public PropertyMappingDto {
        targetProperty = targetProperty == null ? null : targetProperty.trim();
        suggestedExtendedProperty = ExtendedPropertySupport.normalizeSuggestedExtendedProperty(
                suggestedExtendedProperty,
                targetProperty
        );
        dataType = dataType == null ? PropertyDataType.STRING : dataType;
        confidence = confidence == null ? 0 : confidence;
        reason = reason == null ? "" : reason;
        isExtended = isExtended != null ? isExtended : ExtendedPropertySupport.isExtendedProperty(targetProperty);
    }
}
