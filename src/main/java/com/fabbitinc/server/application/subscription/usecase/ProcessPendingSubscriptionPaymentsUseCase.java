package com.fabbitinc.server.application.subscription.usecase;

import com.fabbitinc.server.application.subscription.port.BillingPaymentPort;
import com.fabbitinc.server.application.subscription.port.BillingPaymentPort.PaymentInput;
import com.fabbitinc.server.application.subscription.port.BillingPaymentPort.PaymentLineItem;
import com.fabbitinc.server.application.subscription.port.BillingPaymentPort.PaymentResult;
import com.fabbitinc.server.application.subscription.usecase.result.ProcessPendingSubscriptionPaymentsResult;
import com.fabbitinc.server.domain.subscription.model.StorageOverageLedger;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedger;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerStatus;
import com.fabbitinc.server.domain.subscription.model.SubscriptionBillingLedgerType;
import com.fabbitinc.server.domain.subscription.repository.StorageOverageLedgerRepository;
import com.fabbitinc.server.domain.subscription.repository.SubscriptionBillingLedgerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProcessPendingSubscriptionPaymentsUseCase {

    private final SubscriptionBillingLedgerRepository subscriptionBillingLedgerRepository;
    private final StorageOverageLedgerRepository storageOverageLedgerRepository;
    private final BillingPaymentPort billingPaymentPort;

    public ProcessPendingSubscriptionPaymentsResult execute() {
        List<SubscriptionBillingLedger> pendingLedgers = subscriptionBillingLedgerRepository.findByStatusOrderByCreatedAtAsc(
                SubscriptionBillingLedgerStatus.PENDING
        );
        Map<UUID, List<SubscriptionBillingLedger>> ledgersByOrgId = pendingLedgers.stream()
                .collect(Collectors.groupingBy(SubscriptionBillingLedger::getOrgId));

        int successCount = 0;
        int failureCount = 0;
        int settledLedgerCount = 0;

        for (Map.Entry<UUID, List<SubscriptionBillingLedger>> entry : ledgersByOrgId.entrySet()) {
            UUID orgId = entry.getKey();
            List<SubscriptionBillingLedger> orgLedgers = entry.getValue();
            BigDecimal totalAmount = orgLedgers.stream()
                    .map(SubscriptionBillingLedger::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalAmount.signum() <= 0) {
                orgLedgers.forEach(SubscriptionBillingLedger::markSettled);
                settleStorageOverageLedgers(orgId, orgLedgers);
                successCount++;
                settledLedgerCount += orgLedgers.size();
                log.info(
                        "event=subscription_payment_settled_without_charge org_id={} ledger_count={} total_amount={} outcome=success",
                        orgId,
                        orgLedgers.size(),
                        totalAmount
                );
                continue;
            }

            PaymentResult result = billingPaymentPort.pay(toPaymentInput(orgId, orgLedgers));

            if (!result.success()) {
                failureCount++;
                continue;
            }

            orgLedgers.forEach(SubscriptionBillingLedger::markSettled);
            settleStorageOverageLedgers(orgId, orgLedgers);

            successCount++;
            settledLedgerCount += orgLedgers.size();
            log.info(
                    "event=subscription_payment_settled org_id={} ledger_count={} transaction_id={} outcome=success",
                    orgId,
                    orgLedgers.size(),
                    result.transactionId()
            );
        }

        return new ProcessPendingSubscriptionPaymentsResult(successCount, failureCount, settledLedgerCount);
    }

    private PaymentInput toPaymentInput(UUID orgId, List<SubscriptionBillingLedger> ledgers) {
        BigDecimal totalAmount = ledgers.stream()
                .map(SubscriptionBillingLedger::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = ledgers.stream()
                .map(SubscriptionBillingLedger::getCurrency)
                .findFirst()
                .orElse("KRW");

        List<PaymentLineItem> items = ledgers.stream()
                .map(ledger -> new PaymentLineItem(
                        ledger.getId(),
                        ledger.getLedgerType().name(),
                        ledger.getTotalAmount()
                ))
                .toList();
        return new PaymentInput(orgId, currency, totalAmount, items);
    }

    private void settleStorageOverageLedgers(UUID orgId, List<SubscriptionBillingLedger> orgLedgers) {
        boolean hasStorageOverageLedger = orgLedgers.stream()
                .anyMatch(ledger -> ledger.getLedgerType() == SubscriptionBillingLedgerType.STORAGE_OVERAGE);
        if (!hasStorageOverageLedger) {
            return;
        }

        List<StorageOverageLedger> pendingOverages = storageOverageLedgerRepository.findByOrgIdAndStatus(
                orgId,
                SubscriptionBillingLedgerStatus.PENDING
        );
        pendingOverages.forEach(StorageOverageLedger::markSettled);
    }
}
