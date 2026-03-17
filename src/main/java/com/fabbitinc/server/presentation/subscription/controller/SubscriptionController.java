package com.fabbitinc.server.presentation.subscription.controller;

import com.fabbitinc.server.application.subscription.query.SubscriptionQuery;
import com.fabbitinc.server.application.subscription.query.result.CurrentSubscriptionResult;
import com.fabbitinc.server.application.subscription.usecase.UpdateSubscriptionAiLimitUseCase;
import com.fabbitinc.server.application.subscription.usecase.UpdateSubscriptionPlanUseCase;
import com.fabbitinc.server.application.subscription.usecase.command.UpdateSubscriptionAiLimitCommand;
import com.fabbitinc.server.application.subscription.usecase.command.UpdateSubscriptionPlanCommand;
import com.fabbitinc.server.presentation.subscription.dto.request.UpdateSubscriptionAiLimitRequest;
import com.fabbitinc.server.presentation.subscription.dto.request.UpdateSubscriptionPlanRequest;
import com.fabbitinc.server.presentation.subscription.dto.response.CurrentSubscriptionDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscription")
@Tag(name = "subscription", description = "현재 구독 조회 API")
public class SubscriptionController {

    private final SubscriptionQuery subscriptionQuery;
    private final UpdateSubscriptionPlanUseCase updateSubscriptionPlanUseCase;
    private final UpdateSubscriptionAiLimitUseCase updateSubscriptionAiLimitUseCase;

    @Operation(
            summary = "현재 구독 요약 조회",
            description = "현재 로그인한 워크스페이스의 플랜/좌석/스토리지/AI 정책 요약을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "조직 또는 구독을 찾을 수 없음")
    })
    @GetMapping
    public CurrentSubscriptionDetailResponse get() {
        return toResponse(subscriptionQuery.get());
    }

    @Operation(
            summary = "현재 구독 플랜 변경 예약",
            description = "관리자 권한으로 다음 갱신일부터 적용할 워크스페이스 플랜 변경을 예약합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 예약 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "조직 또는 구독을 찾을 수 없음")
    })
    @PatchMapping("/plan")
    public CurrentSubscriptionDetailResponse updatePlan(
            @Parameter(description = "구독 플랜 변경 요청")
            @Valid @RequestBody UpdateSubscriptionPlanRequest request
    ) {
        updateSubscriptionPlanUseCase.execute(new UpdateSubscriptionPlanCommand(request.planType()));
        return toResponse(subscriptionQuery.get());
    }

    @Operation(
            summary = "현재 구독 AI 한도 정책 변경",
            description = "관리자 권한으로 월간 AI 한도와 초과 즉시 차단 여부를 변경합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정책 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "조직 또는 구독을 찾을 수 없음")
    })
    @PatchMapping("/ai-limit")
    public CurrentSubscriptionDetailResponse updateAiLimit(
            @Parameter(description = "AI 한도 정책 변경 요청")
            @Valid @RequestBody UpdateSubscriptionAiLimitRequest request
    ) {
        updateSubscriptionAiLimitUseCase.execute(
                new UpdateSubscriptionAiLimitCommand(request.aiMonthlyCreditLimit(), request.aiHardLimitEnabled())
        );
        return toResponse(subscriptionQuery.get());
    }

    private CurrentSubscriptionDetailResponse toResponse(CurrentSubscriptionResult result) {
        return new CurrentSubscriptionDetailResponse(
                result.planType(),
                result.status(),
                result.billingCycle(),
                result.currentPeriodStart(),
                result.currentPeriodEnd(),
                result.scheduledPlanType(),
                result.scheduledChangeEffectiveAt(),
                result.usedMembers(),
                result.storageBytesUsed(),
                result.storageBytesIncluded(),
                result.storageBytesOverage(),
                result.allowStorageOverage(),
                result.aiBillingMode(),
                result.starterMonthlyAiCredits(),
                result.aiMonthlyCreditLimit(),
                result.aiHardLimitEnabled(),
                result.seatAllocations().stream()
                        .map(item -> new CurrentSubscriptionDetailResponse.SeatAllocationResponse(
                                item.seatType(),
                                item.assignedCount(),
                                item.purchasedQuantity()
                        ))
                        .toList()
        );
    }
}
