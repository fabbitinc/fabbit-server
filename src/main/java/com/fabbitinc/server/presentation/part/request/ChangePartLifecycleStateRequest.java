package com.fabbitinc.server.presentation.part.request;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "부품 수명주기 상태 변경 요청")
public record ChangePartLifecycleStateRequest(
        @Schema(description = "대상 상태", example = "EOL")
        @NotNull(message = "targetState는 필수입니다")
        PartLifecycleState targetState
) {
}
