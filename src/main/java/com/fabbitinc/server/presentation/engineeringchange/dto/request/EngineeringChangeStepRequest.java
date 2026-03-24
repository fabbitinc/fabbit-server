package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepAssigneeType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeStepType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "변경관리 단계 지정 요청")
public record EngineeringChangeStepRequest(
        @NotNull(message = "stepType은 필수입니다") @Schema(description = "단계 타입", example = "REVIEW")
        EngineeringChangeStepType stepType,
        @NotNull(message = "assigneeType은 필수입니다") @Schema(description = "담당자 타입", example = "USER")
        EngineeringChangeStepAssigneeType assigneeType,
        @NotNull(message = "assigneeId는 필수입니다") @Schema(description = "담당자 ID")
        UUID assigneeId,
        @Min(value = 1, message = "sequence는 1 이상이어야 합니다") @Schema(description = "단계 순서", example = "1")
        int sequence
) {
}
