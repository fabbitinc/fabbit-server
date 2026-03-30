package com.fabbitinc.server.presentation.usage.controller;

import com.fabbitinc.server.application.usage.query.UsageQuery;
import com.fabbitinc.server.application.usage.query.condition.StorageTrendCondition;
import com.fabbitinc.server.application.usage.query.result.CreditUsageResult;
import com.fabbitinc.server.application.usage.query.result.StorageTrendResult;
import com.fabbitinc.server.application.usage.query.result.StorageUsageResult;
import com.fabbitinc.server.presentation.usage.dto.response.CreditUsageResponse;
import com.fabbitinc.server.presentation.usage.dto.response.StorageTrendResponse;
import com.fabbitinc.server.presentation.usage.dto.response.StorageUsageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
            operationId = "usageGetStorage",
            summary = "스토리지 총 사용량/한도/초과분과 카테고리별 내역을 조회합니다",
            description = "스토리지 총 사용량/한도/초과분과 카테고리별 내역을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/storage")
    public StorageUsageResponse getStorageUsage() {
        return toStorageUsageResponse(usageQuery.getStorageUsage());
    }

    @Operation(
            operationId = "usageGetStorageTrend",
            summary = "스토리지 사용량 추이를 period(7d|30d|1y) 기준으로 조회합니다",
            description = "스토리지 사용량 추이를 period(7d|30d|1y) 기준으로 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/storage/trend")
    public StorageTrendResponse getStorageTrend(
            @Parameter(description = "조회 기간 (7d, 30d, 1y)", example = "30d")
            @RequestParam(value = "period", defaultValue = "30d") String period
    ) {
        return toStorageTrendResponse(usageQuery.getStorageTrend(new StorageTrendCondition(period)));
    }

    @Operation(
            operationId = "usageGetCredit",
            summary = "AI 크레딧 잔여/사용량과 카테고리별 사용량을 조회합니다",
            description = "AI 크레딧 잔여/사용량과 카테고리별 사용량을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
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
                result.includedCreditsLimit(),
                result.includedCreditsUsed(),
                result.includedCreditsRemaining(),
                result.meteredCreditsLimit(),
                result.hardLimitEnabled(),
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
