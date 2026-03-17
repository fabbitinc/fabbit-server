package com.fabbitinc.server.infrastructure.external.payment;

import com.fabbitinc.server.application.subscription.port.BillingPaymentPort;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingBillingPaymentAdapter implements BillingPaymentPort {

    @Override
    public PaymentResult pay(PaymentInput input) {
        String transactionId = "mock-pg-" + UUID.randomUUID();
        log.info(
                "event=subscription_payment_completed org_id={} total_amount={} currency={} item_count={} transaction_id={} outcome=success",
                input.orgId(),
                input.totalAmount(),
                input.currency(),
                input.items().size(),
                transactionId
        );
        return new PaymentResult(true, transactionId);
    }
}
