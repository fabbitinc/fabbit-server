package com.fabbitinc.server.application.subscription.service;

import com.fabbitinc.server.application.auth.api.AuthInvitationApi;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.subscription.service.input.UpgradeStarterSubscriptionInput;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.aiusage.model.AiUsageEvent;
import com.fabbitinc.server.domain.aiusage.repository.AiUsageEventRepository;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.subscription.model.AiBillingMode;
import com.fabbitinc.server.domain.subscription.model.BillingCycle;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.Subscription;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedger;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerType;
import com.fabbitinc.server.domain.subscription.model.SubscriptionChangeRequest;
import com.fabbitinc.server.domain.subscription.model.SubscriptionSeatAssignment;
import com.fabbitinc.server.domain.subscription.model.SubscriptionSeatQuota;
import com.fabbitinc.server.domain.subscription.model.SubscriptionCreditPurchase;
import com.fabbitinc.server.domain.subscription.model.SubscriptionChangeRequestStatus;
import com.fabbitinc.server.domain.subscription.model.SubscriptionStatus;
import com.fabbitinc.server.domain.subscription.model.SubscriptionUsagePolicy;
import com.fabbitinc.server.domain.subscription.model.StorageOverageLedger;
import com.fabbitinc.server.domain.subscription.model.StorageUsageSnapshot;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import com.fabbitinc.server.domain.subscription.repository.StorageOverageLedgerRepository;
import com.fabbitinc.server.domain.subscription.repository.StorageUsageSnapshotRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionBillingLedgerRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionChangeRequestRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionCreditPurchaseRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionSeatAssignmentRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionSeatQuotaRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionUsagePolicyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String CURRENCY_KRW = "KRW";
    private static final BigDecimal METERED_AI_CREDIT_UNIT_PRICE = BigDecimal.valueOf(5L).setScale(2, RoundingMode.HALF_UP);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionSeatQuotaRepository subscriptionSeatQuotaRepository;
    private final SubscriptionSeatAssignmentRepository subscriptionSeatAssignmentRepository;
    private final SubscriptionUsagePolicyRepository subscriptionUsagePolicyRepository;
    private final AuthInvitationApi authInvitationApi;
    private final AiUsageEventRepository aiUsageEventRepository;
    private final SubscriptionChangeRequestRepository subscriptionChangeRequestRepository;
    private final SubscriptionCreditPurchaseRepository subscriptionCreditPurchaseRepository;
    private final SubscriptionBillingLedgerRepository subscriptionBillingLedgerRepository;
    private final StorageUsageSnapshotRepository storageUsageSnapshotRepository;
    private final StorageOverageLedgerRepository storageOverageLedgerRepository;

    public Subscription createInitialSubscription(
            UUID orgId,
            WorkspacePlanType planType,
            Membership ownerMembership,
            SeatType ownerSeatType,
            UUID assignedBy
    ) {
        return subscriptionRepository.findByOrgIdAndStatus(orgId, SubscriptionStatus.ACTIVE)
                .orElseGet(() -> createActiveSubscription(orgId, planType, ownerMembership, ownerSeatType, assignedBy));
    }

    public WorkspacePlanType getCurrentPlanType(UUID orgId) {
        return getActiveSubscription(orgId).getPlanType();
    }

    public SeatType getCurrentSeatType(UUID orgId, UUID userId) {
        return subscriptionSeatAssignmentRepository.findByOrgIdAndUserId(orgId, userId)
                .map(SubscriptionSeatAssignment::getSeatType)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "좌석 배정 정보를 찾을 수 없습니다"));
    }

    public SubscriptionUsagePolicy getUsagePolicy(UUID orgId) {
        Subscription subscription = getActiveSubscription(orgId);
        return subscriptionUsagePolicyRepository.findBySubscriptionId(subscription.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "구독 사용량 정책을 찾을 수 없습니다"));
    }

    public Map<UUID, SeatType> getSeatTypesByOrgId(UUID orgId) {
        return subscriptionSeatAssignmentRepository.findByOrgId(orgId).stream()
                .collect(Collectors.toMap(SubscriptionSeatAssignment::getUserId, SubscriptionSeatAssignment::getSeatType));
    }

    public long calculateIncludedStorageBytes(UUID orgId) {
        Subscription subscription = getActiveSubscription(orgId);
        SubscriptionUsagePolicy usagePolicy = getUsagePolicy(orgId);
        long fullSeatCount = subscriptionSeatAssignmentRepository.countByOrgIdAndSeatType(orgId, SeatType.FULL);
        return usagePolicy.calculateIncludedStorageBytes(fullSeatCount);
    }

    public boolean allowsStorageOverage(UUID orgId) {
        return getCurrentPlanType(orgId).allowsStorageOverage();
    }

    public void assertCanAddMember(UUID orgId, int currentUsedMembers) {
        WorkspacePlanType planType = getCurrentPlanType(orgId);
        if (planType.maxMembers() != -1 && currentUsedMembers >= planType.maxMembers()) {
            throw new AppException(ErrorCode.MEMBER_LIMIT_EXCEEDED, "멤버 수 한도를 초과했습니다. Team 플랜 이상으로 업그레이드해주세요.");
        }
    }

    public void assignSeatToMembership(UUID orgId, Membership membership, SeatType requestedSeatType, UUID assignedBy) {
        Subscription subscription = getActiveSubscription(orgId);
        SeatType seatType = resolveAcceptedInvitationSeatType(subscription.getPlanType(), requestedSeatType);
        subscriptionSeatAssignmentRepository.findByMembershipId(membership.getId())
                .ifPresentOrElse(
                        assignment -> assignment.changeSeatType(seatType, assignedBy, Instant.now()),
                        () -> subscriptionSeatAssignmentRepository.save(SubscriptionSeatAssignment.create(
                                subscription.getId(),
                                orgId,
                                membership.getId(),
                                membership.getUserId(),
                                seatType,
                                assignedBy,
                                Instant.now()
                        ))
                );
    }

    public SeatType changeSeatType(UUID orgId, Membership membership, SeatType requestedSeatType, UUID assignedBy) {
        Subscription subscription = getActiveSubscription(orgId);
        WorkspacePlanType planType = subscription.getPlanType();
        SeatType seatType = requireAssignableSeatType(planType, membership, requestedSeatType);
        Instant now = Instant.now();

        SubscriptionSeatAssignment assignment = subscriptionSeatAssignmentRepository.findByMembershipId(membership.getId())
                .orElse(null);

        if (assignment == null) {
            assertSeatQuotaAvailable(subscription, seatType, null);
            subscriptionSeatAssignmentRepository.save(SubscriptionSeatAssignment.create(
                    subscription.getId(),
                    orgId,
                    membership.getId(),
                    membership.getUserId(),
                    seatType,
                    assignedBy,
                    now
            ));
            return seatType;
        }

        SeatType previousSeatType = assignment.getSeatType();
        if (previousSeatType == seatType) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "이미 해당 좌석 타입이 배정되어 있습니다");
        }

        assertSeatQuotaAvailable(subscription, seatType, assignment.getId());
        assignment.changeSeatType(seatType, assignedBy, now);
        return seatType;
    }

    public void removeSeatAssignment(UUID membershipId) {
        subscriptionSeatAssignmentRepository.findByMembershipId(membershipId)
                .ifPresent(subscriptionSeatAssignmentRepository::delete);
    }

    public SeatType resolveInvitationSeatType(UUID orgId, SeatType requestedSeatType) {
        Subscription subscription = getActiveSubscription(orgId);
        WorkspacePlanType planType = subscription.getPlanType();
        if (planType.isStarter()) {
            return SeatType.STARTER;
        }
        if (requestedSeatType == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유료 플랜 초대에는 좌석 타입을 선택해야 합니다");
        }
        if (requestedSeatType == SeatType.STARTER) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유료 플랜에서는 STARTER 좌석으로 초대할 수 없습니다");
        }
        assertSeatQuotaAvailable(subscription, requestedSeatType, null);
        return requestedSeatType;
    }

    public List<SubscriptionSeatQuota> updateSeatQuotas(UUID orgId, Map<SeatType, Integer> requestedSeatQuantities) {
        Subscription subscription = getActiveSubscription(orgId);
        WorkspacePlanType planType = subscription.getPlanType();
        if (planType.isStarter()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Starter 플랜에서는 좌석 수량을 변경할 수 없습니다");
        }

        Map<SeatType, Integer> normalizedQuantities = normalizeRequestedSeatQuantities(requestedSeatQuantities);
        for (SeatType seatType : purchasableSeatTypes()) {
            int assignedCount = Math.toIntExact(subscriptionSeatAssignmentRepository.countByOrgIdAndSeatType(orgId, seatType));
            int reservedCount = Math.toIntExact(authInvitationApi.countPendingInvitations(orgId, seatType));
            int minimumRequired = assignedCount + reservedCount;
            int requestedQuantity = normalizedQuantities.getOrDefault(seatType, 0);
            if (requestedQuantity < minimumRequired) {
                throw new AppException(
                        ErrorCode.QUOTA_EXCEEDED,
                        "이미 배정되었거나 예약된 좌석보다 적게 설정할 수 없습니다: " + seatType.name()
                );
            }
        }

        Map<SeatType, Integer> currentQuantities = purchasableSeatTypes().stream()
                .collect(Collectors.toMap(
                        seatType -> seatType,
                        seatType -> subscriptionSeatQuotaRepository.findBySubscriptionIdAndSeatType(subscription.getId(), seatType)
                                .map(SubscriptionSeatQuota::getPurchasedQuantity)
                                .orElse(0)
                ));
        Instant now = Instant.now();
        for (SeatType seatType : purchasableSeatTypes()) {
            int requestedQuantity = normalizedQuantities.getOrDefault(seatType, 0);
            SubscriptionSeatQuota quota = subscriptionSeatQuotaRepository.findBySubscriptionIdAndSeatTypeForUpdate(subscription.getId(), seatType)
                    .orElse(null);

            if (quota == null) {
                subscriptionSeatQuotaRepository.save(SubscriptionSeatQuota.create(
                        subscription.getId(),
                        seatType,
                        requestedQuantity,
                        planType.seatPrice(seatType),
                        CURRENCY_KRW
                ));
            } else {
                quota.changePurchasedQuantity(requestedQuantity);
            }
        }

        createSeatQuotaAdjustmentLedgers(subscription, planType, currentQuantities, normalizedQuantities, now);

        return subscriptionSeatQuotaRepository.findBySubscriptionId(subscription.getId());
    }

    public void upgradeStarterSubscription(UpgradeStarterSubscriptionInput input) {
        Subscription subscription = getActiveSubscription(input.orgId());
        if (!subscription.getPlanType().isStarter()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Starter 플랜에서만 즉시 업그레이드할 수 있습니다");
        }

        WorkspacePlanType targetPlanType = requireStarterUpgradeTargetPlanType(input.targetPlanType());
        List<UpgradeStarterSubscriptionInput.MemberSeatSelection> memberSeatSelections = requireStarterUpgradeSelections(
                input.memberSeatSelections()
        );
        validatePendingInvitationsAbsent(input.orgId());

        SubscriptionUsagePolicy usagePolicy = subscriptionUsagePolicyRepository.findBySubscriptionId(subscription.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "구독 사용량 정책을 찾을 수 없습니다"));

        Instant now = Instant.now();
        subscription.changePlan(targetPlanType);
        usagePolicy.applyPlanDefaults(targetPlanType);

        Map<SeatType, Integer> seatCounts = new HashMap<>();
        for (UpgradeStarterSubscriptionInput.MemberSeatSelection selection : memberSeatSelections) {
            Membership membership = selection.membership();
            SeatType seatType = requirePaidSeatType(selection.seatType());
            subscriptionSeatAssignmentRepository.findByMembershipId(membership.getId())
                    .ifPresentOrElse(
                            assignment -> assignment.changeSeatType(seatType, input.actorUserId(), now),
                            () -> subscriptionSeatAssignmentRepository.save(SubscriptionSeatAssignment.create(
                                    subscription.getId(),
                                    input.orgId(),
                                    membership.getId(),
                                    membership.getUserId(),
                                    seatType,
                                    input.actorUserId(),
                                    now
                            ))
                    );
            seatCounts.merge(seatType, 1, Integer::sum);
        }

        replaceSeatQuotas(subscription, targetPlanType, seatCounts);
        createSeatQuotaAdjustmentLedgers(subscription, targetPlanType, Map.of(), seatCounts, now);
    }

    public void checkAiUsageAllowance(UUID orgId, AiUsageCategory category) {
        Subscription subscription = getActiveSubscription(orgId);
        SubscriptionUsagePolicy usagePolicy = getUsagePolicy(orgId);
        BigDecimal totalUsed = getUsedCredits(orgId, subscription.getCurrentPeriodStart());
        BigDecimal nextUsage = totalUsed.add(category.creditCostDecimal());

        if (subscription.getPlanType().isStarter()) {
            if (nextUsage.compareTo(usagePolicy.getStarterMonthlyAiCredits()) > 0) {
                throw new AppException(ErrorCode.QUOTA_EXCEEDED, "Starter 플랜의 월간 AI 크레딧을 초과했습니다. Team 플랜 이상으로 업그레이드해주세요.");
            }
            return;
        }

        if (usagePolicy.isAiHardLimitEnabled()
                && usagePolicy.getAiMonthlyCreditLimit() != null
                && nextUsage.compareTo(usagePolicy.getAiMonthlyCreditLimit()) > 0) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED, "설정된 월간 AI 한도를 초과했습니다.");
        }
    }

    public BigDecimal getUsedCredits(UUID orgId, Instant periodStart) {
        BigDecimal total = aiUsageEventRepository.sumCreditsUsed(orgId, periodStart);
        return total == null ? BigDecimal.ZERO : total;
    }

    public SubscriptionChangeRequest schedulePlanChange(
            UUID orgId,
            WorkspacePlanType requestedPlanType,
            Instant effectiveAt,
            Map<String, Object> metadata
    ) {
        Subscription subscription = getActiveSubscription(orgId);
        if (subscription.getPlanType().isStarter()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Starter 플랜 업그레이드는 전용 업그레이드 API를 사용해야 합니다");
        }
        if (subscription.getPlanType() == requestedPlanType) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "현재 플랜과 동일한 플랜으로 변경할 수 없습니다");
        }
        subscription.schedulePlanChange(requestedPlanType, effectiveAt);
        return subscriptionChangeRequestRepository.save(
                SubscriptionChangeRequest.schedule(subscription.getId(), requestedPlanType, effectiveAt, metadata)
        );
    }

    public SubscriptionChangeRequest schedulePlanChangeAtCurrentPeriodEnd(
            UUID orgId,
            WorkspacePlanType requestedPlanType,
            Map<String, Object> metadata
    ) {
        Subscription subscription = getActiveSubscription(orgId);
        return schedulePlanChange(orgId, requestedPlanType, subscription.getCurrentPeriodEnd(), metadata);
    }

    public SubscriptionUsagePolicy updateAiLimit(
            UUID orgId,
            BigDecimal aiMonthlyCreditLimit,
            boolean aiHardLimitEnabled
    ) {
        Subscription subscription = getActiveSubscription(orgId);
        if (subscription.getPlanType().isStarter()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Starter 플랜에서는 AI 한도 정책을 수정할 수 없습니다");
        }
        if (aiHardLimitEnabled && aiMonthlyCreditLimit == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "AI 즉시 차단을 사용하려면 월간 AI 한도를 함께 설정해야 합니다");
        }

        SubscriptionUsagePolicy usagePolicy = getUsagePolicy(orgId);
        try {
            usagePolicy.changeAiLimit(aiMonthlyCreditLimit, aiHardLimitEnabled);
        } catch (DomainException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
        }
        return usagePolicy;
    }

    public SubscriptionCreditPurchase purchaseAiCredits(
            UUID orgId,
            BigDecimal credits,
            BigDecimal unitPrice,
            Instant expiresAt
    ) {
        Subscription subscription = getActiveSubscription(orgId);
        BigDecimal normalizedCredits = credits.setScale(4, RoundingMode.HALF_UP);
        BigDecimal normalizedUnitPrice = unitPrice.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalAmount = normalizedCredits.multiply(normalizedUnitPrice).setScale(2, RoundingMode.HALF_UP);

        SubscriptionCreditPurchase purchase = subscriptionCreditPurchaseRepository.save(
                SubscriptionCreditPurchase.create(
                        subscription.getId(),
                        orgId,
                        normalizedCredits,
                        normalizedUnitPrice,
                        totalAmount,
                        CURRENCY_KRW,
                        expiresAt
                )
        );
        subscriptionBillingLedgerRepository.save(
                SubscriptionBillingLedger.create(
                        subscription.getId(),
                        orgId,
                        SubscriptionBillingLedgerType.AI_CREDIT_PURCHASE,
                        subscription.getCurrentPeriodStart(),
                        subscription.getCurrentPeriodEnd(),
                        normalizedCredits,
                        normalizedUnitPrice.setScale(2, RoundingMode.HALF_UP),
                        totalAmount,
                        CURRENCY_KRW,
                        "subscription_credit_purchase",
                        purchase.getId(),
                        Map.of("creditsPurchased", normalizedCredits)
                )
        );
        return purchase;
    }

    public StorageUsageSnapshot recordStorageSnapshot(UUID orgId, Instant snapshotAt, long totalFileBytes) {
        Subscription subscription = getActiveSubscription(orgId);
        StorageUsageSnapshot existingSnapshot = storageUsageSnapshotRepository.findByOrgIdAndSnapshotAt(orgId, snapshotAt)
                .orElse(null);
        if (existingSnapshot != null) {
            return existingSnapshot;
        }
        SubscriptionUsagePolicy usagePolicy = getUsagePolicy(orgId);
        long fullSeatCount = subscriptionSeatAssignmentRepository.countByOrgIdAndSeatType(orgId, SeatType.FULL);
        long includedBytes = usagePolicy.calculateIncludedStorageBytes(fullSeatCount);
        long overageBytes = Math.max(totalFileBytes - includedBytes, 0L);
        long billableBytes = overageBytes;

        StorageUsageSnapshot snapshot = storageUsageSnapshotRepository.save(
                StorageUsageSnapshot.create(
                        orgId,
                        snapshotAt,
                        totalFileBytes,
                        includedBytes,
                        billableBytes,
                        overageBytes,
                        Map.of("fullSeatCount", fullSeatCount)
                )
        );

        if (overageBytes > 0L) {
            BigDecimal billableGb = BigDecimal.valueOf(overageBytes)
                    .divide(BigDecimal.valueOf(WorkspacePlanType.STORAGE_OVERAGE_UNIT_BYTES), 6, RoundingMode.CEILING);
            BigDecimal unitPrice = WorkspacePlanType.STORAGE_OVERAGE_UNIT_PRICE.setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = billableGb.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            storageOverageLedgerRepository.save(
                    StorageOverageLedger.create(
                            subscription.getId(),
                            orgId,
                            snapshot.getId(),
                            subscription.getCurrentPeriodStart(),
                            subscription.getCurrentPeriodEnd(),
                            overageBytes,
                            billableGb,
                            unitPrice,
                            totalAmount,
                            CURRENCY_KRW
                    )
            );
            subscriptionBillingLedgerRepository.save(
                    SubscriptionBillingLedger.create(
                            subscription.getId(),
                            orgId,
                            SubscriptionBillingLedgerType.STORAGE_OVERAGE,
                            subscription.getCurrentPeriodStart(),
                            subscription.getCurrentPeriodEnd(),
                            billableGb,
                            unitPrice,
                            totalAmount,
                            CURRENCY_KRW,
                            "storage_usage_snapshot",
                            snapshot.getId(),
                            Map.of(
                                    "snapshotAt", snapshotAt.toString(),
                                    "overageBytes", overageBytes
                            )
                    )
            );
        }

        return snapshot;
    }

    public List<Subscription> getDueSubscriptions(Instant renewedAt) {
        return subscriptionRepository.findByStatusInAndCurrentPeriodEndLessThanEqual(
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE),
                renewedAt
        );
    }

    public SubscriptionRenewalResult renewSubscription(UUID orgId, Instant renewedAt) {
        Subscription subscription = getActiveOrPastDueSubscription(orgId);
        Instant currentPeriodStart = subscription.getCurrentPeriodStart();
        Instant currentPeriodEnd = subscription.getCurrentPeriodEnd();

        billMeteredAiUsage(subscription, currentPeriodStart, currentPeriodEnd);

        if (subscription.isCancelAtPeriodEnd()) {
            subscription.cancel();
            return new SubscriptionRenewalResult(false, true);
        }

        if (subscription.getScheduledPlanType() != null
                && subscription.getScheduledChangeEffectiveAt() != null
                && !subscription.getScheduledChangeEffectiveAt().isAfter(renewedAt)) {
            applyScheduledPlanChange(subscription, subscription.getScheduledPlanType(), renewedAt);
            subscriptionChangeRequestRepository.findBySubscriptionIdAndStatus(
                            subscription.getId(),
                            SubscriptionChangeRequestStatus.SCHEDULED
                    )
                    .forEach(SubscriptionChangeRequest::markApplied);
        }

        Instant nextPeriodStart = currentPeriodEnd;
        Instant nextPeriodEnd = switch (subscription.getBillingCycle()) {
            case MONTHLY -> ZonedDateTime.ofInstant(nextPeriodStart, ZoneOffset.UTC).plusMonths(1).toInstant();
            case YEARLY -> ZonedDateTime.ofInstant(nextPeriodStart, ZoneOffset.UTC).plusYears(1).toInstant();
        };

        createSeatBillingLedgers(subscription, nextPeriodStart, nextPeriodEnd);
        subscription.renew(nextPeriodStart, nextPeriodEnd);
        return new SubscriptionRenewalResult(true, false);
    }

    public PlanChangeExecutionResult applyDueScheduledPlanChanges(Instant appliedAt) {
        List<Subscription> dueSubscriptions = subscriptionRepository.findByStatusAndScheduledChangeEffectiveAtLessThanEqual(
                SubscriptionStatus.ACTIVE,
                appliedAt
        );

        int appliedCount = 0;
        int failedCount = 0;

        for (Subscription subscription : dueSubscriptions) {
            WorkspacePlanType requestedPlanType = subscription.getScheduledPlanType();
            if (requestedPlanType == null) {
                continue;
            }

            List<SubscriptionChangeRequest> scheduledRequests = subscriptionChangeRequestRepository.findBySubscriptionIdAndStatus(
                    subscription.getId(),
                    SubscriptionChangeRequestStatus.SCHEDULED
            );

            try {
                applyScheduledPlanChange(subscription, requestedPlanType, appliedAt);
                scheduledRequests.forEach(SubscriptionChangeRequest::markApplied);
                appliedCount++;
            } catch (AppException ex) {
                subscription.clearScheduledPlanChange();
                scheduledRequests.forEach(SubscriptionChangeRequest::markFailed);
                failedCount++;
            }
        }

        return new PlanChangeExecutionResult(appliedCount, failedCount);
    }

    private Subscription createActiveSubscription(
            UUID orgId,
            WorkspacePlanType planType,
            Membership ownerMembership,
            SeatType ownerSeatType,
            UUID assignedBy
    ) {
        WorkspacePlanType resolvedPlanType = requireWorkspacePlanType(planType);
        Instant now = Instant.now();
        Instant periodEnd = ZonedDateTime.ofInstant(now, ZoneOffset.UTC)
                .plusMonths(1)
                .toInstant();

        Subscription subscription = subscriptionRepository.save(Subscription.create(
                orgId,
                resolvedPlanType,
                SubscriptionStatus.ACTIVE,
                BillingCycle.MONTHLY,
                now,
                periodEnd
        ));

        subscriptionUsagePolicyRepository.save(SubscriptionUsagePolicy.create(
                subscription.getId(),
                resolvedPlanType.baseStorageBytes(),
                resolvedPlanType.extraStorageBytesPerFullSeat(),
                WorkspacePlanType.STORAGE_OVERAGE_UNIT_BYTES,
                WorkspacePlanType.STORAGE_OVERAGE_UNIT_PRICE,
                BigDecimal.valueOf(resolvedPlanType.starterMonthlyAiCredits()),
                resolvedPlanType.aiBillingMode(),
                null,
                resolvedPlanType.aiBillingMode() == AiBillingMode.INCLUDED_ONLY
        ));

        SeatType resolvedOwnerSeatType = resolveInitialOwnerSeatType(resolvedPlanType, ownerSeatType);
        int initialQuantity = resolvedPlanType.isStarter() ? resolvedPlanType.maxMembers() : 1;
        subscriptionSeatQuotaRepository.save(SubscriptionSeatQuota.create(
                subscription.getId(),
                resolvedOwnerSeatType,
                initialQuantity,
                resolvedPlanType.seatPrice(resolvedOwnerSeatType),
                CURRENCY_KRW
        ));
        subscriptionSeatAssignmentRepository.save(SubscriptionSeatAssignment.create(
                subscription.getId(),
                orgId,
                ownerMembership.getId(),
                ownerMembership.getUserId(),
                resolvedOwnerSeatType,
                assignedBy,
                now
        ));
        createSeatBillingLedgers(subscription, subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd());
        return subscription;
    }

    private Subscription getActiveOrPastDueSubscription(UUID orgId) {
        return subscriptionRepository.findByOrgIdAndStatus(orgId, SubscriptionStatus.ACTIVE)
                .or(() -> subscriptionRepository.findByOrgIdAndStatus(orgId, SubscriptionStatus.PAST_DUE))
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "활성 구독 정보를 찾을 수 없습니다"));
    }

    private Subscription getActiveSubscription(UUID orgId) {
        return subscriptionRepository.findByOrgIdAndStatus(orgId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "활성 구독 정보를 찾을 수 없습니다"));
    }

    private SeatType requireAssignableSeatType(WorkspacePlanType planType, Membership membership, SeatType requestedSeatType) {
        if (requestedSeatType == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 좌석 타입입니다");
        }
        if (planType.isStarter()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Starter 플랜에서는 좌석 타입을 변경할 수 없습니다");
        }
        if (requestedSeatType == SeatType.STARTER) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유료 플랜에서는 STARTER 좌석을 사용할 수 없습니다");
        }
        if (membership.getRole() == MembershipRole.OWNER && requestedSeatType == SeatType.STARTER) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "소유자에게 STARTER 좌석을 배정할 수 없습니다");
        }
        return requestedSeatType;
    }

    private void applyScheduledPlanChange(Subscription subscription, WorkspacePlanType requestedPlanType, Instant appliedAt) {
        List<SubscriptionSeatAssignment> assignments = subscriptionSeatAssignmentRepository.findBySubscriptionId(subscription.getId());
        validatePlanChange(subscription, assignments, requestedPlanType);

        SubscriptionUsagePolicy usagePolicy = subscriptionUsagePolicyRepository.findBySubscriptionId(subscription.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "구독 사용량 정책을 찾을 수 없습니다"));

        if (requestedPlanType.isStarter()) {
            assignments.forEach(assignment -> assignment.changeSeatType(SeatType.STARTER, null, appliedAt));
        }

        subscription.changePlan(requestedPlanType);
        usagePolicy.applyPlanDefaults(requestedPlanType);
        rebuildSeatQuotas(subscription, requestedPlanType);
    }

    private void validatePlanChange(
            Subscription subscription,
            List<SubscriptionSeatAssignment> assignments,
            WorkspacePlanType requestedPlanType
    ) {
        if (!requestedPlanType.isStarter()) {
            return;
        }
        if (assignments.size() > WorkspacePlanType.STARTER.maxMembers()) {
            throw new AppException(ErrorCode.MEMBER_LIMIT_EXCEEDED, "Starter 플랜은 최대 5명까지만 사용할 수 있습니다");
        }
        int pendingInvitationCount = authInvitationApi.countPendingInvitationsBySeatType(subscription.getOrgId()).values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        if (pendingInvitationCount > 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "대기 중인 초대가 있으면 Starter 플랜으로 변경할 수 없습니다");
        }
    }

    private WorkspacePlanType requireStarterUpgradeTargetPlanType(WorkspacePlanType targetPlanType) {
        if (targetPlanType == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "업그레이드 대상 플랜은 필수입니다");
        }
        if (targetPlanType != WorkspacePlanType.TEAM && targetPlanType != WorkspacePlanType.ORGANIZATION) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Starter 업그레이드는 Team 또는 Org 플랜만 지원합니다");
        }
        return targetPlanType;
    }

    private List<UpgradeStarterSubscriptionInput.MemberSeatSelection> requireStarterUpgradeSelections(
            List<UpgradeStarterSubscriptionInput.MemberSeatSelection> memberSeatSelections
    ) {
        if (memberSeatSelections == null || memberSeatSelections.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "업그레이드할 멤버 좌석 정보는 필수입니다");
        }
        return memberSeatSelections;
    }

    private void validatePendingInvitationsAbsent(UUID orgId) {
        int pendingInvitationCount = authInvitationApi.countPendingInvitationsBySeatType(orgId).values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        if (pendingInvitationCount > 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "대기 중인 초대가 있으면 Starter 업그레이드를 진행할 수 없습니다");
        }
    }

    private SeatType requirePaidSeatType(SeatType seatType) {
        if (seatType == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유료 플랜 좌석 타입은 필수입니다");
        }
        if (seatType == SeatType.STARTER) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Starter 업그레이드에서는 STARTER 좌석을 사용할 수 없습니다");
        }
        return seatType;
    }

    private void rebuildSeatQuotas(
            Subscription subscription,
            WorkspacePlanType planType
    ) {
        List<SubscriptionSeatQuota> existingQuotas = subscriptionSeatQuotaRepository.findBySubscriptionId(subscription.getId());
        if (!existingQuotas.isEmpty()) {
            subscriptionSeatQuotaRepository.deleteAll(existingQuotas);
        }

        if (planType.isStarter()) {
            subscriptionSeatQuotaRepository.save(SubscriptionSeatQuota.create(
                    subscription.getId(),
                    SeatType.STARTER,
                    planType.maxMembers(),
                    planType.seatPrice(SeatType.STARTER),
                    CURRENCY_KRW
            ));
            return;
        }

        Map<SeatType, Integer> countsBySeatType = new HashMap<>();
        existingQuotas.stream()
                .filter(quota -> quota.getSeatType() != SeatType.STARTER)
                .forEach(quota -> countsBySeatType.put(quota.getSeatType(), quota.getPurchasedQuantity()));

        Arrays.stream(SeatType.values())
                .filter(seatType -> seatType != SeatType.STARTER)
                .filter(countsBySeatType::containsKey)
                .forEach(seatType -> subscriptionSeatQuotaRepository.save(SubscriptionSeatQuota.create(
                        subscription.getId(),
                        seatType,
                        countsBySeatType.get(seatType),
                        planType.seatPrice(seatType),
                        CURRENCY_KRW
                )));
    }

    private void replaceSeatQuotas(
            Subscription subscription,
            WorkspacePlanType planType,
            Map<SeatType, Integer> seatCounts
    ) {
        List<SubscriptionSeatQuota> existingQuotas = subscriptionSeatQuotaRepository.findBySubscriptionId(subscription.getId());
        if (!existingQuotas.isEmpty()) {
            subscriptionSeatQuotaRepository.deleteAll(existingQuotas);
        }

        for (SeatType seatType : purchasableSeatTypes()) {
            int purchasedQuantity = seatCounts.getOrDefault(seatType, 0);
            if (purchasedQuantity <= 0) {
                continue;
            }
            subscriptionSeatQuotaRepository.save(SubscriptionSeatQuota.create(
                    subscription.getId(),
                    seatType,
                    purchasedQuantity,
                    planType.seatPrice(seatType),
                    CURRENCY_KRW
            ));
        }
    }

    private SeatType resolveInitialOwnerSeatType(WorkspacePlanType planType, SeatType requestedSeatType) {
        if (planType.isStarter()) {
            return SeatType.STARTER;
        }
        if (requestedSeatType == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유료 플랜에서는 생성자의 좌석 타입을 선택해야 합니다");
        }
        if (requestedSeatType == SeatType.STARTER) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유료 플랜에서는 STARTER 좌석을 사용할 수 없습니다");
        }
        return requestedSeatType;
    }

    private SeatType resolveAcceptedInvitationSeatType(
            WorkspacePlanType planType,
            SeatType invitationSeatType
    ) {
        if (planType.isStarter()) {
            return SeatType.STARTER;
        }
        if (invitationSeatType == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "초대 좌석 타입은 필수입니다");
        }
        if (invitationSeatType == SeatType.STARTER) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유료 플랜에서는 STARTER 좌석을 배정할 수 없습니다");
        }
        return invitationSeatType;
    }

    private void assertSeatQuotaAvailable(Subscription subscription, SeatType seatType, UUID assignmentIdToExclude) {
        if (subscription.getPlanType().isStarter()) {
            return;
        }

        SubscriptionSeatQuota quota = subscriptionSeatQuotaRepository.findBySubscriptionIdAndSeatTypeForUpdate(subscription.getId(), seatType)
                .orElseThrow(() -> new AppException(ErrorCode.QUOTA_EXCEEDED, "구매한 좌석이 없습니다: " + seatType.name()));
        int assignedCount = Math.toIntExact(subscriptionSeatAssignmentRepository.countByOrgIdAndSeatType(subscription.getOrgId(), seatType));
        int reservedCount = Math.toIntExact(authInvitationApi.countPendingInvitations(subscription.getOrgId(), seatType));

        if (assignmentIdToExclude != null) {
            SubscriptionSeatAssignment assignment = subscriptionSeatAssignmentRepository.findById(assignmentIdToExclude).orElse(null);
            if (assignment != null && assignment.getSeatType() == seatType) {
                assignedCount = Math.max(0, assignedCount - 1);
            }
        }

        if (quota.getPurchasedQuantity() <= assignedCount + reservedCount) {
            throw new AppException(ErrorCode.QUOTA_EXCEEDED, "가용 좌석이 없습니다: " + seatType.name());
        }
    }

    private Map<SeatType, Integer> normalizeRequestedSeatQuantities(Map<SeatType, Integer> requestedSeatQuantities) {
        Map<SeatType, Integer> normalized = new HashMap<>();
        if (requestedSeatQuantities == null) {
            return normalized;
        }
        for (Map.Entry<SeatType, Integer> entry : requestedSeatQuantities.entrySet()) {
            SeatType seatType = entry.getKey();
            Integer quantity = entry.getValue();
            if (seatType == null || seatType == SeatType.STARTER) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 좌석 타입입니다");
            }
            if (quantity == null || quantity < 0) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "좌석 수량은 0 이상이어야 합니다");
            }
            normalized.put(seatType, quantity);
        }
        return normalized;
    }

    private List<SeatType> purchasableSeatTypes() {
        return List.of(SeatType.VIEWER, SeatType.COLLABORATOR, SeatType.FULL);
    }

    private WorkspacePlanType requireWorkspacePlanType(WorkspacePlanType planType) {
        if (planType == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 플랜입니다");
        }
        return planType;
    }

    private void billMeteredAiUsage(Subscription subscription, Instant periodStart, Instant periodEnd) {
        SubscriptionUsagePolicy usagePolicy = subscriptionUsagePolicyRepository.findBySubscriptionId(subscription.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND, "구독 사용량 정책을 찾을 수 없습니다"));
        if (!usagePolicy.getAiBillingMode().isMetered()) {
            return;
        }
        if (subscriptionBillingLedgerRepository.existsBySubscriptionIdAndLedgerTypeAndPeriodStartAndPeriodEnd(
                subscription.getId(),
                SubscriptionBillingLedgerType.AI_USAGE,
                periodStart,
                periodEnd
        )) {
            return;
        }

        List<AiUsageEvent> pendingEvents = runInTenantContext(
                subscription.getOrgId(),
                () -> aiUsageEventRepository.findByOrgIdAndBillingStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        subscription.getOrgId(),
                        AiUsageEvent.BILLING_STATUS_PENDING,
                        periodStart,
                        periodEnd
                )
        );
        if (pendingEvents.isEmpty()) {
            return;
        }

        BigDecimal totalCredits = runInTenantContext(
                subscription.getOrgId(),
                () -> aiUsageEventRepository.sumCreditsUsed(
                        subscription.getOrgId(),
                        periodStart,
                        periodEnd,
                        AiUsageEvent.BILLING_STATUS_PENDING
                )
        );
        BigDecimal totalAmount = runInTenantContext(
                subscription.getOrgId(),
                () -> aiUsageEventRepository.sumBillableAmount(
                        subscription.getOrgId(),
                        periodStart,
                        periodEnd,
                        AiUsageEvent.BILLING_STATUS_PENDING
                )
        ).setScale(2, RoundingMode.HALF_UP);

        if (totalCredits.signum() == 0) {
            runInTenantContext(subscription.getOrgId(), () -> {
                pendingEvents.forEach(event -> event.markBilled(BigDecimal.ZERO));
                aiUsageEventRepository.saveAll(pendingEvents);
                return null;
            });
            return;
        }

        subscriptionBillingLedgerRepository.save(
                SubscriptionBillingLedger.create(
                        subscription.getId(),
                        subscription.getOrgId(),
                        SubscriptionBillingLedgerType.AI_USAGE,
                        periodStart,
                        periodEnd,
                        totalCredits,
                        METERED_AI_CREDIT_UNIT_PRICE,
                        totalAmount,
                        CURRENCY_KRW,
                        "ai_usage_period",
                        null,
                        Map.of("usageEventCount", pendingEvents.size())
                )
        );
        runInTenantContext(subscription.getOrgId(), () -> {
            pendingEvents.forEach(event -> event.markBilled(event.getBillableAmount()));
            aiUsageEventRepository.saveAll(pendingEvents);
            return null;
        });
    }

    private <T> T runInTenantContext(UUID orgId, Supplier<T> supplier) {
        TenantContextHolder.setCurrentSchema(TenantSchemaPolicy.schemaNameForOrgId(orgId));
        try {
            return supplier.get();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void createSeatBillingLedgers(Subscription subscription, Instant periodStart, Instant periodEnd) {
        List<SubscriptionSeatQuota> quotas = subscriptionSeatQuotaRepository.findBySubscriptionId(subscription.getId());
        for (SubscriptionSeatQuota quota : quotas) {
            if (quota.getPurchasedQuantity() <= 0 || quota.getUnitPrice() <= 0) {
                continue;
            }
            String referenceType = "seat_" + quota.getSeatType().name().toLowerCase();
            if (subscriptionBillingLedgerRepository.existsBySubscriptionIdAndLedgerTypeAndPeriodStartAndPeriodEndAndReferenceType(
                    subscription.getId(),
                    SubscriptionBillingLedgerType.SEAT,
                    periodStart,
                    periodEnd,
                    referenceType
            )) {
                continue;
            }

            BigDecimal quantity = BigDecimal.valueOf(quota.getPurchasedQuantity()).setScale(6, RoundingMode.HALF_UP);
            BigDecimal unitAmount = BigDecimal.valueOf(quota.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = unitAmount.multiply(quantity).setScale(2, RoundingMode.HALF_UP);

            subscriptionBillingLedgerRepository.save(
                    SubscriptionBillingLedger.create(
                            subscription.getId(),
                            subscription.getOrgId(),
                            SubscriptionBillingLedgerType.SEAT,
                            periodStart,
                            periodEnd,
                            quantity,
                            unitAmount,
                            totalAmount,
                            CURRENCY_KRW,
                            referenceType,
                            null,
                            Map.of("seatType", quota.getSeatType().name())
                    )
            );
        }
    }

    private void createSeatQuotaAdjustmentLedgers(
            Subscription subscription,
            WorkspacePlanType planType,
            Map<SeatType, Integer> previousQuantities,
            Map<SeatType, Integer> updatedQuantities,
            Instant changedAt
    ) {
        BigDecimal remainingRatio = subscription.calculateRemainingBillingRatio(changedAt);
        if (remainingRatio.signum() == 0) {
            return;
        }

        for (SeatType seatType : purchasableSeatTypes()) {
            int previousQuantity = previousQuantities.getOrDefault(seatType, 0);
            int updatedQuantity = updatedQuantities.getOrDefault(seatType, 0);
            int delta = updatedQuantity - previousQuantity;
            if (delta == 0) {
                continue;
            }

            BigDecimal quantity = BigDecimal.valueOf(Math.abs(delta)).setScale(6, RoundingMode.HALF_UP);
            BigDecimal unitAmount = BigDecimal.valueOf(planType.seatPrice(seatType)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal proratedAmount = unitAmount.multiply(quantity)
                    .multiply(remainingRatio)
                    .setScale(2, RoundingMode.HALF_UP);
            if (proratedAmount.signum() == 0) {
                continue;
            }
            BigDecimal signedAmount = delta > 0 ? proratedAmount : proratedAmount.negate();

            subscriptionBillingLedgerRepository.save(
                    SubscriptionBillingLedger.create(
                            subscription.getId(),
                            subscription.getOrgId(),
                            SubscriptionBillingLedgerType.ADJUSTMENT,
                            subscription.getCurrentPeriodStart(),
                            subscription.getCurrentPeriodEnd(),
                            quantity,
                            unitAmount,
                            signedAmount,
                            CURRENCY_KRW,
                            "seat_proration_" + seatType.name().toLowerCase(),
                            null,
                            Map.of(
                                    "seatType", seatType.name(),
                                    "changeType", delta > 0 ? "UPGRADE_OR_ADD" : "DOWNGRADE_OR_REMOVE",
                                    "delta", delta,
                                    "remainingRatio", remainingRatio,
                                    "changedAt", changedAt.toString()
                            )
                    )
            );
        }
    }

    public record PlanChangeExecutionResult(
            int appliedCount,
            int failedCount
    ) {
    }

    public record SubscriptionRenewalResult(
            boolean renewed,
            boolean canceled
    ) {
    }
}
