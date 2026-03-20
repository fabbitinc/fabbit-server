package com.fabbitinc.server.presentation.subscription.controller;

import com.fabbitinc.server.application.subscription.query.SubscriptionQuery;
import com.fabbitinc.server.application.subscription.query.result.CurrentSubscriptionResult;
import com.fabbitinc.server.application.subscription.usecase.UpgradeStarterSubscriptionUseCase;
import com.fabbitinc.server.application.subscription.usecase.UpdateSubscriptionAiLimitUseCase;
import com.fabbitinc.server.application.subscription.usecase.UpdateSubscriptionPlanUseCase;
import com.fabbitinc.server.application.subscription.usecase.UpdateSubscriptionSeatQuotasUseCase;
import com.fabbitinc.server.application.subscription.usecase.command.UpgradeStarterSubscriptionCommand;
import com.fabbitinc.server.application.subscription.usecase.command.UpdateSubscriptionAiLimitCommand;
import com.fabbitinc.server.application.subscription.usecase.command.UpdateSubscriptionPlanCommand;
import com.fabbitinc.server.application.subscription.usecase.command.UpdateSubscriptionSeatQuotasCommand;
import com.fabbitinc.server.presentation.subscription.dto.request.UpgradeStarterSubscriptionRequest;
import com.fabbitinc.server.presentation.subscription.dto.request.UpdateSubscriptionAiLimitRequest;
import com.fabbitinc.server.presentation.subscription.dto.request.UpdateSubscriptionPlanRequest;
import com.fabbitinc.server.presentation.subscription.dto.request.UpdateSubscriptionSeatQuotasRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscription")
@Tag(name = "subscription", description = "현재 구독 조회/업그레이드/변경 API")
public class SubscriptionController {

    private final SubscriptionQuery subscriptionQuery;
    private final UpgradeStarterSubscriptionUseCase upgradeStarterSubscriptionUseCase;
    private final UpdateSubscriptionPlanUseCase updateSubscriptionPlanUseCase;
    private final UpdateSubscriptionAiLimitUseCase updateSubscriptionAiLimitUseCase;
    private final UpdateSubscriptionSeatQuotasUseCase updateSubscriptionSeatQuotasUseCase;

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
            description = "관리자 권한으로 이미 유료 플랜인 워크스페이스의 다음 갱신 시점 플랜 변경을 예약합니다. Starter에서 유료 플랜으로의 즉시 전환은 starter-upgrade API를 사용합니다"
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
            description = "관리자 권한으로 유료 플랜의 월간 AI 한도와 하드 리밋 여부를 변경합니다"
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

    @Operation(
            summary = "Starter 플랜 즉시 업그레이드",
            description = "현재 Starter 워크스페이스를 Team 또는 Organization으로 즉시 전환하고, 기존 멤버 전원의 좌석 타입을 한 번에 확정합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업그레이드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "조직 또는 구독을 찾을 수 없음")
    })
    @PostMapping("/starter-upgrade")
    public CurrentSubscriptionDetailResponse upgradeStarter(
            @Parameter(description = "Starter 플랜 즉시 업그레이드 요청")
            @Valid @RequestBody UpgradeStarterSubscriptionRequest request
    ) {
        upgradeStarterSubscriptionUseCase.execute(new UpgradeStarterSubscriptionCommand(
                request.targetPlanType(),
                request.memberSeats().stream()
                        .map(item -> new UpgradeStarterSubscriptionCommand.MemberSeatCommand(
                                item.membershipId(),
                                item.seatType()
                        ))
                        .toList()
        ));
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
                                item.reservedCount(),
                                item.purchasedQuantity(),
                                item.availableQuantity(),
                                item.unitPrice()
                        ))
                        .toList()
        );
    }

    @Operation(
            summary = "현재 구독 좌석 수량 변경",
            description = "관리자 권한으로 유료 플랜 워크스페이스에서 구매한 좌석 수량을 변경합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "좌석 수량 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "조직 또는 구독을 찾을 수 없음")
    })
    @PatchMapping("/seat-quotas")
    public CurrentSubscriptionDetailResponse updateSeatQuotas(
            @Parameter(description = "좌석 수량 변경 요청")
            @Valid @RequestBody UpdateSubscriptionSeatQuotasRequest request
    ) {
        updateSubscriptionSeatQuotasUseCase.execute(new UpdateSubscriptionSeatQuotasCommand(
                request.seatQuotas().stream()
                        .map(item -> new UpdateSubscriptionSeatQuotasCommand.SeatQuantityCommand(
                                item.seatType(),
                                item.purchasedQuantity()
                        ))
                        .toList()
        ));
        return toResponse(subscriptionQuery.get());
    }
}
