package com.fabbitinc.server.presentation.auth.dto.response;

import com.fabbitinc.server.domain.subscription.model.AiBillingMode;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import io.swagger.v3.oas.annotations.media.Schema;

public record PlanResponse(
        @Schema(description = "플랜 타입", example = "STARTER")
        WorkspacePlanType planType,
        @Schema(description = "플랜 표시 이름", example = "Starter")
        String displayName,
        @Schema(description = "플랜 설명", example = "소규모 팀을 위한 기본 플랜")
        String description,
        @Schema(description = "최대 멤버 수", example = "10")
        int maxMembers,
        @Schema(description = "기본 스토리지 바이트", example = "10000000000")
        long baseStorageBytes,
        @Schema(description = "Full Seat당 추가 스토리지 바이트", example = "10000000000")
        long extraStorageBytesPerFullSeat,
        @Schema(description = "Starter 월 포함 AI 크레딧", example = "100")
        int starterMonthlyAiCredits,
        @Schema(description = "AI 과금 방식", example = "METERED")
        AiBillingMode aiBillingMode,
        @Schema(description = "Viewer 월 과금 금액", example = "5000")
        int viewerMonthlyPrice,
        @Schema(description = "Collaborator 월 과금 금액", example = "15000")
        int collaboratorMonthlyPrice,
        @Schema(description = "Full Seat 월 과금 금액", example = "29000")
        int fullSeatMonthlyPrice,
        @Schema(description = "스토리지 초과 1GB당 월 과금 금액", example = "200")
        int storageOveragePricePerGb
) {
}
