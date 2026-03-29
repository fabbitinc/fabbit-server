package com.fabbitinc.server.presentation.bom.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Schema(description = "BOM 항목 추가 요청")
public record AddBomItemRequest(

        @Schema(description = "하위 부품 리비전 ID")
        @NotNull(message = "하위 부품 리비전 ID는 필수입니다")
        UUID childPartRevisionId,

        @Schema(description = "BOM 줄 번호", example = "10")
        @NotBlank(message = "BOM 줄 번호는 필수입니다")
        @Size(max = 50, message = "BOM 줄 번호는 최대 50자여야 합니다")
        String lineNumber,

        @Schema(description = "수량", example = "2")
        @NotNull(message = "수량은 필수입니다")
        @DecimalMin(value = "0", inclusive = false, message = "수량은 0보다 커야 합니다")
        BigDecimal quantity,

        @Schema(
                description = "확장 속성 JSON 객체. key는 property_definition.id(UUID)여야 합니다",
                example = "{}"
        )
        Map<String, Object> extendedProperties
) {
}
