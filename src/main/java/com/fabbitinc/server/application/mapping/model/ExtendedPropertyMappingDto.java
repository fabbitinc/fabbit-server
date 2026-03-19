package com.fabbitinc.server.application.mapping.model;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "확장 속성 매핑")
public record ExtendedPropertyMappingDto(
        @Schema(description = "원본 컬럼명", example = "비고")
        String sourceColumn,
        @Schema(
                description = "백엔드가 내부 생성하는 확장 속성 키",
                example = "_ext_remark",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String generatedKey,
        @Schema(description = "데이터 타입", example = "string")
        PropertyDataType dataType
) {
    public ExtendedPropertyMappingDto {
        sourceColumn = sourceColumn == null ? null : sourceColumn.trim();
        generatedKey = generatedKey == null ? null : generatedKey.trim();
        dataType = dataType == null ? PropertyDataType.STRING : dataType;
    }
}
