package com.fabbitinc.server.domain.subscription.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubscriptionBillingLedgerRelationTest {

    @Test
    void billingLedger는_음수_totalAmount를_허용한다() {
        SubscriptionBillingLedger ledger = SubscriptionBillingLedger.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                SubscriptionBillingLedgerType.ADJUSTMENT,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                BigDecimal.ONE.setScale(6),
                BigDecimal.valueOf(5_000L).setScale(2),
                BigDecimal.valueOf(-5_000L).setScale(2),
                "KRW",
                "seat_proration_viewer",
                null,
                Map.of("changeType", "DOWNGRADE_OR_REMOVE")
        );

        assertEquals(BigDecimal.valueOf(-5_000L).setScale(2), ledger.getTotalAmount());
    }

    @Test
    void billingLedger는_음수_quantity를_허용하지_않는다() {
        DomainException exception = assertThrows(DomainException.class, () -> SubscriptionBillingLedger.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                SubscriptionBillingLedgerType.ADJUSTMENT,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                BigDecimal.valueOf(-1L).setScale(6),
                BigDecimal.valueOf(5_000L).setScale(2),
                BigDecimal.valueOf(-5_000L).setScale(2),
                "KRW",
                "seat_proration_viewer",
                null,
                Map.of()
        ));

        assertEquals(SubscriptionBillingLedger.CODE_SUBSCRIPTION_BILLING_LEDGER_AMOUNT_INVALID, exception.getDomainCode());
    }

    @Test
    void billingLedger는_음수_unitAmount를_허용하지_않는다() {
        DomainException exception = assertThrows(DomainException.class, () -> SubscriptionBillingLedger.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                SubscriptionBillingLedgerType.ADJUSTMENT,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                BigDecimal.ONE.setScale(6),
                BigDecimal.valueOf(-5_000L).setScale(2),
                BigDecimal.valueOf(-5_000L).setScale(2),
                "KRW",
                "seat_proration_viewer",
                null,
                Map.of()
        ));

        assertEquals(SubscriptionBillingLedger.CODE_SUBSCRIPTION_BILLING_LEDGER_AMOUNT_INVALID, exception.getDomainCode());
    }
}
