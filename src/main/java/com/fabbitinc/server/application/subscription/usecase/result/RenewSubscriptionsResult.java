package com.fabbitinc.server.application.subscription.usecase.result;

public record RenewSubscriptionsResult(
        int renewedCount,
        int canceledCount,
        int failedCount
) {
}
