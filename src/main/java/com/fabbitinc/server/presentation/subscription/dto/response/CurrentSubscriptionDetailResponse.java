package com.fabbitinc.server.presentation.subscription.dto.response;

import com.fabbitinc.server.domain.subscription.model.AiBillingMode;
import com.fabbitinc.server.domain.subscription.model.BillingCycle;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "현재 구독 요약 응답")
public record CurrentSubscriptionDetailResponse(
        @Schema(description = "현재 플랜", example = "TEAM")
        WorkspacePlanType planType,
        @Schema(description = "구독 상태", example = "ACTIVE")
        SubscriptionStatus status,
        @Schema(description = "청구 주기", example = "MONTHLY")
        BillingCycle billingCycle,
        @Schema(description = "현재 청구 시작 시각")
        Instant currentPeriodStart,
        @Schema(description = "현재 청구 종료 시각")
        Instant currentPeriodEnd,
        @Schema(description = "예약된 플랜 변경", example = "ORG")
        WorkspacePlanType scheduledPlanType,
        @Schema(description = "예약된 플랜 변경 적용 시각")
        Instant scheduledChangeEffectiveAt,
        @Schema(description = "현재 사용 멤버 수", example = "7")
        int usedMembers,
        @Schema(description = "현재 스토리지 사용량(바이트)", example = "524288000")
        long storageBytesUsed,
        @Schema(description = "현재 포함 스토리지(바이트)", example = "20000000000")
        long storageBytesIncluded,
        @Schema(description = "현재 초과 스토리지(바이트)", example = "0")
        long storageBytesOverage,
        @Schema(description = "스토리지 초과 허용 여부", example = "true")
        boolean allowStorageOverage,
        @Schema(description = "AI 과금 방식", example = "METERED")
        AiBillingMode aiBillingMode,
        @Schema(description = "Starter 월 포함 AI 크레딧", example = "100")
        int starterMonthlyAiCredits,
        @Schema(description = "월간 AI 크레딧 한도", example = "2000")
        Integer aiMonthlyCreditLimit,
        @Schema(description = "AI 한도 초과 시 즉시 차단 여부", example = "true")
        boolean aiHardLimitEnabled,
        @Schema(description = "좌석 할당 요약")
        List<SeatAllocationResponse> seatAllocations
) {
    public record SeatAllocationResponse(
            @Schema(description = "좌석 타입", example = "FULL")
            SeatType seatType,
            @Schema(description = "현재 배정된 좌석 수", example = "3")
            int assignedCount,
            @Schema(description = "대기 중 초대로 예약된 좌석 수", example = "1")
            long reservedCount,
            @Schema(description = "현재 구매 좌석 수", example = "5")
            int purchasedQuantity,
            @Schema(description = "즉시 배정 가능한 좌석 수", example = "1")
            int availableQuantity,
            @Schema(description = "좌석 월 단가", example = "29000")
            int unitPrice
    ) {
    }
}
