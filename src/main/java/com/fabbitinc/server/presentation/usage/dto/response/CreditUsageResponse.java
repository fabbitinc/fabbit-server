package com.fabbitinc.server.presentation.usage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "크레딧 사용량 응답")
public record CreditUsageResponse(
        @Schema(description = "현재 기간 시작 시각")
        Instant currentPeriodStart,
        @Schema(description = "현재 기간 종료 시각")
        Instant currentPeriodEnd,
        @Schema(description = "총 사용 크레딧", example = "120")
        int totalCreditsUsed,
        @Schema(description = "플랜 크레딧 사용량", example = "100")
        int planCreditsUsed,
        @Schema(description = "플랜 크레딧 한도", example = "1000")
        int planCreditsLimit,
        @Schema(description = "플랜 잔여 크레딧", example = "900")
        int planCreditsRemaining,
        @Schema(description = "보너스 크레딧 사용량", example = "20")
        int bonusCreditsUsed,
        @Schema(description = "보너스 잔여 크레딧", example = "80")
        int bonusCreditsRemaining,
        @Schema(description = "카테고리별 사용량 목록")
        List<CreditCategoryItemResponse> categories
) {
    @Schema(description = "크레딧 사용 카테고리 항목")
    public record CreditCategoryItemResponse(
            @Schema(description = "사용 카테고리", example = "OCR")
            String category,
            @Schema(description = "사용 크레딧", example = "50")
            int creditsUsed,
            @Schema(description = "호출 횟수", example = "12")
            long usageCount
    ) {
    }
}
