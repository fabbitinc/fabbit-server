package com.fabbitinc.server.application.subscription.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface BillingPaymentPort {

    PaymentResult pay(PaymentInput input);

    record PaymentInput(
            UUID orgId,
            String currency,
            BigDecimal totalAmount,
            List<PaymentLineItem> items
    ) {
    }

    record PaymentLineItem(
            UUID billingLedgerId,
            String ledgerType,
            BigDecimal amount
    ) {
    }

    record PaymentResult(
            boolean success,
            String transactionId
    ) {
    }
}
