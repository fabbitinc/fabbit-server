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
        @Schema(description = "포함 크레딧 한도", example = "100")
        int includedCreditsLimit,
        @Schema(description = "포함 크레딧 사용량", example = "20")
        int includedCreditsUsed,
        @Schema(description = "포함 크레딧 잔여량", example = "80")
        int includedCreditsRemaining,
        @Schema(description = "월간 AI 한도(없으면 null)", example = "2000")
        Integer meteredCreditsLimit,
        @Schema(description = "월간 AI 한도 초과 시 차단 여부", example = "true")
        boolean hardLimitEnabled,
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
