package com.fabbitinc.server.application.subscription.usecase.result;

public record ApplyScheduledSubscriptionChangesResult(
        int appliedCount,
        int failedCount
) {
}
