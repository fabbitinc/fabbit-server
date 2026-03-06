package com.fabbitinc.server.presentation.usage.controller;

import com.fabbitinc.server.application.usage.query.condition.StorageTrendCondition;
import com.fabbitinc.server.application.usage.query.result.CreditUsageResult;
import com.fabbitinc.server.application.usage.query.result.StorageTrendResult;
import com.fabbitinc.server.application.usage.query.result.StorageUsageResult;
import com.fabbitinc.server.application.usage.query.UsageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.fabbitinc.server.presentation.usage.dto.response.CreditUsageResponse;
import com.fabbitinc.server.presentation.usage.dto.response.StorageTrendResponse;
import com.fabbitinc.server.presentation.usage.dto.response.StorageUsageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/usage")
@Tag(name = "usage", description = "사용량 조회 API")
public class UsageController {

    private final UsageQuery usageQuery;

    @Operation(
            summary = "GET /api/v1/usage/storage",
            description = "스토리지 총 사용량/한도/초과분과 카테고리별 내역을 조회합니다"
    )
    @GetMapping("/storage")
    public StorageUsageResponse getStorageUsage() {
        return toStorageUsageResponse(usageQuery.getStorageUsage());
    }

    @Operation(
            summary = "GET /api/v1/usage/storage/trend",
            description = "스토리지 사용량 추이를 period(7d|30d|1y) 기준으로 조회합니다"
    )
    @GetMapping("/storage/trend")
    public StorageTrendResponse getStorageTrend(
            @RequestParam(value = "period", defaultValue = "30d") String period
    ) {
        return toStorageTrendResponse(usageQuery.getStorageTrend(new StorageTrendCondition(period)));
    }

    @Operation(
            summary = "GET /api/v1/usage/credits",
            description = "AI 크레딧 잔여/사용량과 카테고리별 사용량을 조회합니다"
    )
    @GetMapping("/credits")
    public CreditUsageResponse getCreditUsage() {
        return toCreditUsageResponse(usageQuery.getCreditUsage());
    }

    private StorageUsageResponse toStorageUsageResponse(StorageUsageResult result) {
        return new StorageUsageResponse(
                result.bytesUsed(),
                result.bytesLimit(),
                result.bytesOverage(),
                result.allowOverage(),
                result.categories().stream()
                        .map(item -> new StorageUsageResponse.StorageCategoryItemResponse(
                                item.category(),
                                item.bytesUsed(),
                                item.fileCount()
                        ))
                        .toList()
        );
    }

    private StorageTrendResponse toStorageTrendResponse(StorageTrendResult result) {
        return new StorageTrendResponse(
                result.items().stream()
                        .map(item -> new StorageTrendResponse.StorageTrendItemResponse(
                                item.date(),
                                item.drawing(),
                                item.attachment(),
                                item.other()
                        ))
                        .toList()
        );
    }

    private CreditUsageResponse toCreditUsageResponse(CreditUsageResult result) {
        return new CreditUsageResponse(
                result.currentPeriodStart(),
                result.currentPeriodEnd(),
                result.totalCreditsUsed(),
                result.planCreditsUsed(),
                result.planCreditsLimit(),
                result.planCreditsRemaining(),
                result.bonusCreditsUsed(),
                result.bonusCreditsRemaining(),
                result.categories().stream()
                        .map(item -> new CreditUsageResponse.CreditCategoryItemResponse(
                                item.category(),
                                item.creditsUsed(),
                                item.usageCount()
                        ))
                        .toList()
        );
    }
}
