package com.fabbitinc.server.application.subscription.usecase.result;

public record ProcessPendingSubscriptionPaymentsResult(
        int successCount,
        int failureCount,
        int settledLedgerCount
) {
}
