package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "변경관리 영향 항목 대상")
public record EngineeringChangeAffectedItemTargetRequest(
        @Schema(description = "항목 유형", example = "REVISION_RELEASE")
        @NotNull(message = "itemType은 필수입니다") EngineeringChangeAffectedItemType itemType,
        @Schema(description = "대상 ID (리비전 ID 또는 부품 ID)")
        @NotNull(message = "targetId는 필수입니다") UUID targetId,
        @Schema(description = "lifecycle 변경 시 대상 상태", example = "EOL")
        PartLifecycleState targetState
) {
}
