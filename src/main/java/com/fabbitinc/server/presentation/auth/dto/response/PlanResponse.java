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
        @Schema(description = "최대 멤버 수, -1이면 코드상 명시적 상한 없음", example = "5")
        int maxMembers,
        @Schema(description = "플랜 기본 제공 스토리지 바이트", example = "250000000")
        long baseStorageBytes,
        @Schema(description = "Full 좌석 1개당 추가 스토리지 바이트", example = "10000000000")
        long extraStorageBytesPerFullSeat,
        @Schema(description = "플랜별 스토리지 초과 허용 여부", example = "false")
        boolean allowStorageOverage,
        @Schema(description = "현재 가입 흐름에서 바로 선택 가능한 플랜인지 여부", example = "true")
        boolean availableForSignup,
        @Schema(description = "Starter 즉시 업그레이드 대상 플랜으로 사용할 수 있는지 여부", example = "false")
        boolean availableForStarterUpgrade,
        @Schema(description = "현재 문의/상담 경로가 필요한 플랜인지 여부", example = "false")
        boolean contactRequired,
        @Schema(description = "Starter 월 포함 AI 크레딧", example = "100")
        int starterMonthlyAiCredits,
        @Schema(description = "AI 과금 방식, Starter는 INCLUDED_ONLY이고 유료 플랜은 METERED", example = "INCLUDED_ONLY")
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
