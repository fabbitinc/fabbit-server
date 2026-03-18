package com.fabbitinc.server.application.subscription.api;

import com.fabbitinc.server.application.subscription.service.SubscriptionService;
import com.fabbitinc.server.application.subscription.service.input.UpgradeStarterSubscriptionInput;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.Subscription;
import com.fabbitinc.server.domain.subscription.model.SubscriptionChangeRequest;
import com.fabbitinc.server.domain.subscription.model.SubscriptionCreditPurchase;
import com.fabbitinc.server.domain.subscription.model.SubscriptionSeatQuota;
import com.fabbitinc.server.domain.subscription.model.SubscriptionUsagePolicy;
import com.fabbitinc.server.domain.subscription.model.StorageUsageSnapshot;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionApi {

    private final SubscriptionService subscriptionService;

    public Subscription createInitialSubscription(
            UUID orgId,
            WorkspacePlanType planType,
            Membership ownerMembership,
            SeatType ownerSeatType,
            UUID assignedBy
    ) {
        return subscriptionService.createInitialSubscription(orgId, planType, ownerMembership, ownerSeatType, assignedBy);
    }

    public WorkspacePlanType getCurrentPlanType(UUID orgId) {
        return subscriptionService.getCurrentPlanType(orgId);
    }

    public SeatType getCurrentSeatType(UUID orgId, UUID userId) {
        return subscriptionService.getCurrentSeatType(orgId, userId);
    }

    public SubscriptionUsagePolicy getUsagePolicy(UUID orgId) {
        return subscriptionService.getUsagePolicy(orgId);
    }

    public Map<UUID, SeatType> getSeatTypesByOrgId(UUID orgId) {
        return subscriptionService.getSeatTypesByOrgId(orgId);
    }

    public long calculateIncludedStorageBytes(UUID orgId) {
        return subscriptionService.calculateIncludedStorageBytes(orgId);
    }

    public boolean allowsStorageOverage(UUID orgId) {
        return subscriptionService.allowsStorageOverage(orgId);
    }

    public void assertCanAddMember(UUID orgId, int currentUsedMembers) {
        subscriptionService.assertCanAddMember(orgId, currentUsedMembers);
    }

    public void assignSeatToMembership(UUID orgId, Membership membership, SeatType seatType, UUID assignedBy) {
        subscriptionService.assignSeatToMembership(orgId, membership, seatType, assignedBy);
    }

    public SeatType changeSeatType(UUID orgId, Membership membership, SeatType requestedSeatType, UUID assignedBy) {
        return subscriptionService.changeSeatType(orgId, membership, requestedSeatType, assignedBy);
    }

    public SeatType resolveInvitationSeatType(UUID orgId, SeatType requestedSeatType) {
        return subscriptionService.resolveInvitationSeatType(orgId, requestedSeatType);
    }

    public void removeSeatAssignment(UUID membershipId) {
        subscriptionService.removeSeatAssignment(membershipId);
    }

    public List<SubscriptionSeatQuota> updateSeatQuotas(UUID orgId, Map<SeatType, Integer> requestedSeatQuantities) {
        return subscriptionService.updateSeatQuotas(orgId, requestedSeatQuantities);
    }

    public void upgradeStarterSubscription(UpgradeStarterSubscriptionInput input) {
        subscriptionService.upgradeStarterSubscription(input);
    }

    public void checkAiUsageAllowance(UUID orgId, AiUsageCategory category) {
        subscriptionService.checkAiUsageAllowance(orgId, category);
    }

    public BigDecimal getUsedCredits(UUID orgId, java.time.Instant periodStart) {
        return subscriptionService.getUsedCredits(orgId, periodStart);
    }

    public SubscriptionChangeRequest schedulePlanChange(
            UUID orgId,
            WorkspacePlanType requestedPlanType,
            Instant effectiveAt,
            Map<String, Object> metadata
    ) {
        return subscriptionService.schedulePlanChange(orgId, requestedPlanType, effectiveAt, metadata);
    }

    public SubscriptionChangeRequest schedulePlanChangeAtCurrentPeriodEnd(
            UUID orgId,
            WorkspacePlanType requestedPlanType,
            Map<String, Object> metadata
    ) {
        return subscriptionService.schedulePlanChangeAtCurrentPeriodEnd(orgId, requestedPlanType, metadata);
    }

    public SubscriptionUsagePolicy updateAiLimit(UUID orgId, BigDecimal aiMonthlyCreditLimit, boolean aiHardLimitEnabled) {
        return subscriptionService.updateAiLimit(orgId, aiMonthlyCreditLimit, aiHardLimitEnabled);
    }

    public SubscriptionCreditPurchase purchaseAiCredits(UUID orgId, BigDecimal credits, BigDecimal unitPrice, Instant expiresAt) {
        return subscriptionService.purchaseAiCredits(orgId, credits, unitPrice, expiresAt);
    }

    public StorageUsageSnapshot recordStorageSnapshot(UUID orgId, Instant snapshotAt, long totalFileBytes) {
        return subscriptionService.recordStorageSnapshot(orgId, snapshotAt, totalFileBytes);
    }
}
