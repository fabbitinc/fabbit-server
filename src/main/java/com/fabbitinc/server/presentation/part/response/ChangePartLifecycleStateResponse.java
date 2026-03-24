package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "부품 수명주기 상태 변경 응답")
public record ChangePartLifecycleStateResponse(
        @Schema(description = "부품 ID")
        UUID partId,
        @Schema(description = "변경된 수명주기 상태")
        PartLifecycleState lifecycleState
) {
}
