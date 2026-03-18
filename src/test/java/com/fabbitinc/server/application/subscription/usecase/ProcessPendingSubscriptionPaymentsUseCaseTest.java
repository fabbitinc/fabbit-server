package com.fabbitinc.server.application.subscription.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.subscription.port.BillingPaymentPort;
import com.fabbitinc.server.application.subscription.port.BillingPaymentPort.PaymentResult;
import com.fabbitinc.server.application.subscription.usecase.result.ProcessPendingSubscriptionPaymentsResult;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedger;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerStatus;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerType;
import com.fabbitinc.server.domain.subscription.repository.StorageOverageLedgerRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionBillingLedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessPendingSubscriptionPaymentsUseCaseTest {

    @Mock
    private SubscriptionBillingLedgerRepository subscriptionBillingLedgerRepository;

    @Mock
    private StorageOverageLedgerRepository storageOverageLedgerRepository;

    @Mock
    private BillingPaymentPort billingPaymentPort;

    @InjectMocks
    private ProcessPendingSubscriptionPaymentsUseCase useCase;

    @Test
    void 합산금액이_0이하이면_pg호출없이_정산완료한다() {
        UUID orgId = UUID.randomUUID();
        SubscriptionBillingLedger creditLedger = SubscriptionBillingLedger.create(
                UUID.randomUUID(),
                orgId,
                SubscriptionBillingLedgerType.ADJUSTMENT,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                BigDecimal.ONE.setScale(6),
                BigDecimal.valueOf(10_000L).setScale(2),
                BigDecimal.valueOf(-10_000L).setScale(2),
                "KRW",
                "seat_proration_viewer",
                null,
                Map.of("changeType", "DOWNGRADE_OR_REMOVE")
        );

        when(subscriptionBillingLedgerRepository.findByStatusOrderByCreatedAtAsc(SubscriptionBillingLedgerStatus.PENDING))
                .thenReturn(List.of(creditLedger));

        ProcessPendingSubscriptionPaymentsResult result = useCase.execute();

        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());
        assertEquals(1, result.settledLedgerCount());
        assertEquals(SubscriptionBillingLedgerStatus.SETTLED, creditLedger.getStatus());
        verifyNoInteractions(billingPaymentPort);
    }

    @Test
    void 합산금액이_양수이면_pg를_호출하고_정산완료한다() {
        UUID orgId = UUID.randomUUID();
        SubscriptionBillingLedger seatLedger = SubscriptionBillingLedger.create(
                UUID.randomUUID(),
                orgId,
                SubscriptionBillingLedgerType.SEAT,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                BigDecimal.ONE.setScale(6),
                BigDecimal.valueOf(29_000L).setScale(2),
                BigDecimal.valueOf(29_000L).setScale(2),
                "KRW",
                "seat_full",
                null,
                Map.of("seatType", "FULL")
        );

        when(subscriptionBillingLedgerRepository.findByStatusOrderByCreatedAtAsc(SubscriptionBillingLedgerStatus.PENDING))
                .thenReturn(List.of(seatLedger));
        when(billingPaymentPort.pay(argThat(input ->
                input.orgId().equals(orgId)
                        && input.totalAmount().compareTo(BigDecimal.valueOf(29_000L)) == 0
                        && input.items().size() == 1
        ))).thenReturn(new PaymentResult(true, "tx-test"));

        ProcessPendingSubscriptionPaymentsResult result = useCase.execute();

        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());
        assertEquals(1, result.settledLedgerCount());
        assertEquals(SubscriptionBillingLedgerStatus.SETTLED, seatLedger.getStatus());
        verify(billingPaymentPort).pay(argThat(input ->
                input.orgId().equals(orgId)
                        && input.totalAmount().compareTo(BigDecimal.valueOf(29_000L)) == 0
                        && input.items().size() == 1
        ));
    }
}
