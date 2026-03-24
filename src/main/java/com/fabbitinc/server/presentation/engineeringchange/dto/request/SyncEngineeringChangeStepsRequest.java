package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "변경관리 단계 동기화 요청")
public record SyncEngineeringChangeStepsRequest(
        @NotNull(message = "steps는 필수입니다") @Valid @Schema(description = "변경관리 단계 목록")
        List<EngineeringChangeStepRequest> steps
) {
    public SyncEngineeringChangeStepsRequest {
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
