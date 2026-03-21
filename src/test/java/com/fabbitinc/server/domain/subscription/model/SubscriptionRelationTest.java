package com.fabbitinc.server.domain.subscription.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionRelationTest {

    @Test
    void subscription_생성시_orgId와_구독정보를_보관한다() {
        UUID orgId = UUID.randomUUID();
        Subscription subscription = Subscription.create(
                orgId,
                WorkspacePlanType.STARTER,
                SubscriptionStatus.ACTIVE,
                BillingCycle.MONTHLY,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z")
        );

        assertEquals(orgId, subscription.getOrgId());
        assertEquals(WorkspacePlanType.STARTER, subscription.getPlanType());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(BillingCycle.MONTHLY, subscription.getBillingCycle());
        assertFalse(subscription.isCancelAtPeriodEnd());
    }

    @Test
    void subscription_orgId가_null이면_예외를_던진다() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        DomainException ex = assertThrows(DomainException.class, () -> Subscription.create(
                null,
                WorkspacePlanType.STARTER,
                SubscriptionStatus.ACTIVE,
                BillingCycle.MONTHLY,
                start,
                end
        ));

        assertEquals(Subscription.CODE_SUBSCRIPTION_ORGANIZATION_REQUIRED, ex.getDomainCode());
    }

    @Test
    void subscription_planType이_null이면_예외를_던진다() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        DomainException ex = assertThrows(DomainException.class, () -> Subscription.create(
                UUID.randomUUID(),
                null,
                SubscriptionStatus.ACTIVE,
                BillingCycle.MONTHLY,
                start,
                end
        ));

        assertEquals(Subscription.CODE_SUBSCRIPTION_PLAN_TYPE_REQUIRED, ex.getDomainCode());
    }

    @Test
    void subscription_종료시각이_시작시각보다_이전이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Subscription.create(
                UUID.randomUUID(),
                WorkspacePlanType.STARTER,
                SubscriptionStatus.ACTIVE,
                BillingCycle.MONTHLY,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-03-01T00:00:00Z")
        ));

        assertEquals(Subscription.CODE_SUBSCRIPTION_PERIOD_INVALID, ex.getDomainCode());
    }

    @Test
    void subscription_정기해지를_예약할수있다() {
        Subscription subscription = createActiveSubscription();

        subscription.scheduleCancelAtPeriodEnd();

        assertTrue(subscription.isCancelAtPeriodEnd());
    }

    @Test
    void subscription_플랜변경예약을_저장할수있다() {
        Subscription subscription = createActiveSubscription();
        Instant effectiveAt = Instant.parse("2026-04-01T00:00:00Z");

        subscription.schedulePlanChange(WorkspacePlanType.ORGANIZATION, effectiveAt);

        assertEquals(WorkspacePlanType.ORGANIZATION, subscription.getScheduledPlanType());
        assertEquals(effectiveAt, subscription.getScheduledChangeEffectiveAt());
    }

    @Test
    void subscription_즉시플랜변경은_예약정보를_초기화한다() {
        Subscription subscription = createActiveSubscription();
        subscription.schedulePlanChange(WorkspacePlanType.ORGANIZATION, Instant.parse("2026-04-01T00:00:00Z"));

        subscription.changePlan(WorkspacePlanType.TEAM);

        assertEquals(WorkspacePlanType.TEAM, subscription.getPlanType());
        assertNull(subscription.getScheduledPlanType());
        assertNull(subscription.getScheduledChangeEffectiveAt());
    }

    @Test
    void subscription_연체후_갱신하면_활성상태와_새_기간으로_복구된다() {
        Subscription subscription = createActiveSubscription();
        Instant newPeriodStart = Instant.parse("2026-04-01T00:00:00Z");
        Instant newPeriodEnd = Instant.parse("2026-05-01T00:00:00Z");

        subscription.scheduleCancelAtPeriodEnd();
        subscription.markPastDue();
        subscription.renew(newPeriodStart, newPeriodEnd);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertEquals(newPeriodStart, subscription.getCurrentPeriodStart());
        assertEquals(newPeriodEnd, subscription.getCurrentPeriodEnd());
        assertFalse(subscription.isCancelAtPeriodEnd());
    }

    @Test
    void subscription_즉시해지하면_cancel예약을_해제한다() {
        Subscription subscription = createActiveSubscription();

        subscription.scheduleCancelAtPeriodEnd();
        subscription.cancel();

        assertEquals(SubscriptionStatus.CANCELED, subscription.getStatus());
        assertFalse(subscription.isCancelAtPeriodEnd());
    }

    @Test
    void subscription_해지된후_갱신하면_예외를_던진다() {
        Subscription subscription = createActiveSubscription();
        subscription.cancel();

        DomainException ex = assertThrows(DomainException.class, () -> subscription.renew(
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:00Z")
        ));

        assertEquals(Subscription.CODE_SUBSCRIPTION_INVALID_STATE, ex.getDomainCode());
    }

    @Test
    void subscription_현재기간_중간시점의_남은_청구비율을_계산할수있다() {
        Subscription subscription = createActiveSubscription();

        BigDecimal ratio = subscription.calculateRemainingBillingRatio(Instant.parse("2026-03-16T12:00:00Z"));

        assertTrue(ratio.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(ratio.compareTo(BigDecimal.ONE) < 0);
        assertEquals(6, ratio.scale());
    }

    @Test
    void subscription_현재기간_시작시점의_남은_청구비율은_1이다() {
        Subscription subscription = createActiveSubscription();

        BigDecimal ratio = subscription.calculateRemainingBillingRatio(Instant.parse("2026-03-01T00:00:00Z"));

        assertEquals(BigDecimal.ONE.setScale(6), ratio);
    }

    @Test
    void subscription_현재기간이_끝난_시점의_남은_청구비율은_0이다() {
        Subscription subscription = createActiveSubscription();

        BigDecimal ratio = subscription.calculateRemainingBillingRatio(Instant.parse("2026-04-01T00:00:00Z"));

        assertEquals(BigDecimal.ZERO.setScale(6), ratio);
    }

    private Subscription createActiveSubscription() {
        return Subscription.create(
                UUID.randomUUID(),
                WorkspacePlanType.STARTER,
                SubscriptionStatus.ACTIVE,
                BillingCycle.MONTHLY,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z")
        );
    }
}
