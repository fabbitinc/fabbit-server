package com.fabbitinc.server.application.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlanResponse(
        @Schema(description = "플랜 타입", example = "STARTER")
        String planType,
        @Schema(description = "플랜 표시 이름", example = "Starter")
        String displayName,
        @Schema(description = "플랜 설명", example = "소규모 팀을 위한 기본 플랜")
        String description,
        @Schema(description = "최대 멤버 수", example = "10")
        int maxMembers,
        @Schema(description = "스토리지(GB)", example = "10")
        int storageGb,
        @Schema(description = "AI 크레딧", example = "1000")
        int aiCredits,
        @Schema(description = "월 과금 금액", example = "0")
        int priceMonthly
) {
}
