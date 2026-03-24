package com.fabbitinc.server.presentation.subscription.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

@Schema(description = "구독 AI 한도 정책 변경 요청")
public record UpdateSubscriptionAiLimitRequest(
        @Schema(description = "월간 AI 크레딧 한도, null이면 한도 미설정", example = "2000")
        @PositiveOrZero BigDecimal aiMonthlyCreditLimit,
        @Schema(description = "월간 AI 한도 초과 시 즉시 차단 여부", example = "true")
        boolean aiHardLimitEnabled
) {
}
