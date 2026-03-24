package com.fabbitinc.server.presentation.part.request;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "부품 생성 요청")
public record CreatePartRequest(
        @Schema(description = "품번", example = "P-100")
        @NotBlank(message = "part_number는 필수입니다") @Size(max = 100, message = "part_number는 최대 100자여야 합니다") String partNumber,

        @Schema(description = "품명", example = "M3 볼트")
        @Size(max = 500, message = "name은 최대 500자여야 합니다") String name,

        @Schema(description = "재질", example = "SUS304")
        @Size(max = 200, message = "material은 최대 200자여야 합니다") String material,

        @Schema(description = "단위", example = "EA")
        @Size(max = 20, message = "unit은 최대 20자여야 합니다") String unit,

        @Schema(description = "설명", example = "체결용 표준 부품")
        String description,

        @Schema(description = "카테고리", example = "FASTENER")
        @Size(max = 100, message = "category는 최대 100자여야 합니다") String category,

        @Schema(description = "팬텀 부품 여부", example = "false")
        Boolean isPhantom,

        @Schema(
                description = "수명주기 상태",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "EOL", "OBSOLETE"}
        )
        PartLifecycleState lifecycleState,

        @Schema(description = "리드타임(일)", example = "7")
        @Min(value = 0, message = "lead_time_days는 0 이상이어야 합니다") Integer leadTimeDays,

        @Schema(
                description = "확장 속성 JSON 객체. key는 property_definition.id(UUID)여야 합니다",
                example = "{\"019d0000-0000-7000-8000-000000000001\":\"AL6061\"}"
        )
        Map<String, Object> extendedProperties,

        @Schema(description = "생성 사유", example = "신규 고객 프로젝트 대응을 위해 부품을 등록합니다")
        @Size(max = 2000, message = "reason은 최대 2000자여야 합니다") String reason
) {
}
