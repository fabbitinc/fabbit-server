package com.fabbitinc.server.application.subscription.query;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.organization.api.OrganizationApi;
import com.fabbitinc.server.application.subscription.query.result.CurrentSubscriptionResult;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.Subscription;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.model.SubscriptionUsagePolicy;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionSeatAssignmentRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionSeatQuotaRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionUsagePolicyRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final OrganizationApi organizationApi;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionSeatAssignmentRepository subscriptionSeatAssignmentRepository;
    private final SubscriptionSeatQuotaRepository subscriptionSeatQuotaRepository;
    private final SubscriptionUsagePolicyRepository subscriptionUsagePolicyRepository;

    public CurrentSubscriptionResult get() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        Organization organization = organizationApi.getOrganizationOrThrow(auth.orgId());
        Subscription subscription = subscriptionRepository.findByOrgIdAndStatus(auth.orgId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "활성 구독 정보를 찾을 수 없습니다"));
        SubscriptionUsagePolicy usagePolicy = subscriptionUsagePolicyRepository.findBySubscriptionId(subscription.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "구독 사용량 정책을 찾을 수 없습니다"));

        long fullSeatCount = subscriptionSeatAssignmentRepository.countByOrgIdAndSeatType(auth.orgId(), SeatType.FULL);
        long includedBytes = usagePolicy.calculateIncludedStorageBytes(fullSeatCount);
        long overageBytes = Math.max(organization.getStorageBytesUsed() - includedBytes, 0L);

        return new CurrentSubscriptionResult(
                subscription.getPlanType(),
                subscription.getStatus(),
                subscription.getBillingCycle(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.getScheduledPlanType(),
                subscription.getScheduledChangeEffectiveAt(),
                organization.getUsedMembers(),
                organization.getStorageBytesUsed(),
                includedBytes,
                overageBytes,
                subscription.getPlanType().allowsStorageOverage(),
                usagePolicy.getAiBillingMode(),
                usagePolicy.getStarterMonthlyAiCredits().intValue(),
                usagePolicy.getAiMonthlyCreditLimit() == null ? null : usagePolicy.getAiMonthlyCreditLimit().intValue(),
                usagePolicy.isAiHardLimitEnabled(),
                Arrays.stream(SeatType.values())
                        .map(seatType -> new CurrentSubscriptionResult.SeatAllocationResult(
                                seatType,
                                (int) subscriptionSeatAssignmentRepository.countByOrgIdAndSeatType(auth.orgId(), seatType),
                                subscriptionSeatQuotaRepository.findBySubscriptionIdAndSeatType(subscription.getId(), seatType)
                                        .map(quota -> quota.getPurchasedQuantity())
                                        .orElse(0)
                        ))
                        .toList()
        );
    }
}
