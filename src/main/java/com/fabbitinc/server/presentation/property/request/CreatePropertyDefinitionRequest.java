package com.fabbitinc.server.presentation.property.request;

import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "커스텀 속성 정의 생성 요청")
public record CreatePropertyDefinitionRequest(
        @Schema(description = "속성 소유 타입", example = "PART")
        @NotNull(message = "owner_type은 필수입니다") String ownerType,

        @Schema(description = "속성 표시명", example = "표면처리")
        @NotBlank(message = "display_name은 필수입니다") @Size(max = 200, message = "display_name은 최대 200자여야 합니다") String displayName,

        @Schema(description = "속성 설명", example = "표면처리 방식")
        String description,

        @Schema(description = "속성 값 타입", example = "OPTION")
        @NotNull(message = "value_type은 필수입니다") PropertyValueType valueType,

        @Schema(description = "옵션 입력 모드", example = "FIXED")
        PropertyOptionMode optionMode,

        @Schema(description = "옵션 목록")
        @Valid List<PropertyOptionRequest> options,

        @Schema(description = "표시 순서", example = "120")
        @Min(value = 0, message = "display_order는 0 이상이어야 합니다") Integer displayOrder,

        @Schema(description = "필수 여부", example = "false")
        Boolean required
) {
}
