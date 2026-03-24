package com.fabbitinc.server.presentation.subscription.dto.request;

import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "구독 플랜 변경 요청")
public record UpdateSubscriptionPlanRequest(
        @Schema(description = "다음 갱신일부터 적용할 플랜", example = "ORGANIZATION")
        @NotNull WorkspacePlanType planType
) {
}
