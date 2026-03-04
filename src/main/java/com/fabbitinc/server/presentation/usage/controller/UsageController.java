package com.fabbitinc.server.presentation.usage.controller;

import com.fabbitinc.server.application.usage.dto.response.CreditUsageResponse;
import com.fabbitinc.server.application.usage.dto.response.StorageTrendResponse;
import com.fabbitinc.server.application.usage.dto.response.StorageUsageResponse;
import com.fabbitinc.server.application.usage.query.UsageQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
        return usageQuery.getStorageUsage();
    }

    @Operation(
            summary = "GET /api/v1/usage/storage/trend",
            description = "스토리지 사용량 추이를 period(7d|30d|1y) 기준으로 조회합니다"
    )
    @GetMapping("/storage/trend")
    public StorageTrendResponse getStorageTrend(
            @RequestParam(value = "period", defaultValue = "30d") String period
    ) {
        return usageQuery.getStorageTrend(period);
    }

    @Operation(
            summary = "GET /api/v1/usage/credits",
            description = "AI 크레딧 잔여/사용량과 카테고리별 사용량을 조회합니다"
    )
    @GetMapping("/credits")
    public CreditUsageResponse getCreditUsage() {
        return usageQuery.getCreditUsage();
    }
}
