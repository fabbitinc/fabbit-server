package com.fabbitinc.server.presentation.property.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "속성 순서 변경 요청")
public record ReorderPropertyRequest(
        @Schema(description = "속성 소유 타입", example = "PART")
        @NotNull(message = "owner_type은 필수입니다") @JsonProperty("owner_type")
        String ownerType,

        @Schema(description = "변경할 최종 속성 순서")
        @NotEmpty(message = "properties는 최소 1개 이상이어야 합니다") @Valid List<ReorderPropertyItemRequest> properties
) {

    @Schema(description = "순서 변경 대상 속성")
    public record ReorderPropertyItemRequest(
            @Schema(description = "속성 key. 시스템 속성은 property_key, 커스텀 속성은 property catalog key(UUID 문자열)", example = "material")
            @NotBlank(message = "property_key는 비어 있을 수 없습니다") @JsonProperty("property_key")
            String propertyKey,

            @Schema(description = "시스템 속성 여부", example = "true")
            @NotNull(message = "system은 필수입니다") Boolean system
    ) {
    }
}
