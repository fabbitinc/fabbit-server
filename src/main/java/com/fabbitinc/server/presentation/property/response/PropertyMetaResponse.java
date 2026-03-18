package com.fabbitinc.server.presentation.property.response;

import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.support.PartSystemPropertyKind;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "속성 메타 응답")
public record PropertyMetaResponse(
        @Schema(description = "커스텀 속성 정의 ID. 시스템 속성이면 null", example = "019d0000-0000-7000-8000-000000000001")
        UUID definitionId,

        @Schema(description = "속성 소유 타입", example = "PART")
        PropertyOwnerType ownerType,

        @Schema(description = "속성 key. 시스템 속성은 property_key, 커스텀 속성은 property_definition.id", example = "category")
        String propertyKey,

        @Schema(description = "시스템 속성 여부", example = "true")
        boolean system,

        @Schema(description = "PART 시스템 속성 종류. PART 시스템 속성이 아니면 null", example = "CATEGORY")
        PartSystemPropertyKind partSystemPropertyKind,

        @Schema(description = "활성 여부를 조직 설정에서 변경할 수 있는지 여부", example = "true")
        boolean activeConfigurable,

        @Schema(description = "시스템 컬럼명. 커스텀 속성이면 null", example = "category")
        String columnName,

        @Schema(description = "표시명", example = "카테고리")
        String displayName,

        @Schema(description = "설명", example = "부품 분류")
        String description,

        @Schema(description = "값 타입", example = "OPTION")
        PropertyValueType valueType,

        @Schema(description = "옵션 입력 모드", example = "CREATABLE")
        PropertyOptionMode optionMode,

        @Schema(description = "옵션 목록")
        List<PropertyOptionResponse> options,

        @Schema(description = "표시 순서", example = "70")
        int displayOrder,

        @Schema(description = "필수 여부", example = "false")
        boolean required,

        @Schema(description = "활성 여부", example = "true")
        boolean active
) {
}
