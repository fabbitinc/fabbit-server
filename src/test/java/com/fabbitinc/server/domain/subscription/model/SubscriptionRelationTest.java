package com.fabbitinc.server.domain.subscription.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionRelationTest {

    @Test
    void subscription_생성시_orgId와_구독정보를_보관한다() {
        UUID orgId = UUID.randomUUID();
        Subscription subscription = Subscription.create(
                orgId,
                "STARTER",
                SubscriptionStatus.ACTIVE,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                10,
                1000,
                1024L
        );

        assertEquals(orgId, subscription.getOrgId());
        assertEquals("STARTER", subscription.getPlanType());
        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertFalse(subscription.isCancelAtPeriodEnd());
    }

    @Test
    void subscription_orgId가_null이면_예외를_던진다() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        DomainException ex = assertThrows(DomainException.class, () -> Subscription.create(
                null,
                "STARTER",
                SubscriptionStatus.ACTIVE,
                start,
                end,
                10,
                1000,
                1024L
        ));

        assertEquals(Subscription.CODE_SUBSCRIPTION_ORG_REQUIRED, ex.getDomainCode());
    }

    @Test
    void subscription_planType이_비어있으면_예외를_던진다() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);

        DomainException ex = assertThrows(DomainException.class, () -> Subscription.create(
                UUID.randomUUID(),
                " ",
                SubscriptionStatus.ACTIVE,
                start,
                end,
                10,
                1000,
                1024L
        ));

        assertEquals(Subscription.CODE_SUBSCRIPTION_PLAN_TYPE_REQUIRED, ex.getDomainCode());
    }

    @Test
    void subscription_종료시각이_시작시각보다_이전이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Subscription.create(
                UUID.randomUUID(),
                "STARTER",
                SubscriptionStatus.ACTIVE,
                Instant.parse("2026-04-01T00:00:00Z"),
                Instant.parse("2026-03-01T00:00:00Z"),
                10,
                1000,
                1024L
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

    private Subscription createActiveSubscription() {
        return Subscription.create(
                UUID.randomUUID(),
                "STARTER",
                SubscriptionStatus.ACTIVE,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                10,
                1000,
                1024L
        );
    }
}
