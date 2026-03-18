package com.fabbitinc.server.infrastructure.scheduling;

import com.fabbitinc.server.application.subscription.usecase.ApplyScheduledSubscriptionChangesUseCase;
import com.fabbitinc.server.application.subscription.usecase.ProcessPendingSubscriptionPaymentsUseCase;
import com.fabbitinc.server.application.subscription.usecase.RecordStorageUsageSnapshotsUseCase;
import com.fabbitinc.server.application.subscription.usecase.RenewSubscriptionsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final ApplyScheduledSubscriptionChangesUseCase applyScheduledSubscriptionChangesUseCase;
    private final ProcessPendingSubscriptionPaymentsUseCase processPendingSubscriptionPaymentsUseCase;
    private final RecordStorageUsageSnapshotsUseCase recordStorageUsageSnapshotsUseCase;
    private final RenewSubscriptionsUseCase renewSubscriptionsUseCase;
    private final ScheduledJobLockSupport scheduledJobLockSupport;

    @Scheduled(cron = "${app.subscription.apply-plan-change-cron:0 */10 * * * *}")
    public void applyScheduledPlanChanges() {
        scheduledJobLockSupport.executeWithLock(
                "subscription_apply_scheduled_plan_changes",
                applyScheduledSubscriptionChangesUseCase::execute
        );
    }

    @Scheduled(cron = "${app.subscription.record-storage-snapshot-cron:0 0 2 * * *}")
    public void recordStorageSnapshots() {
        scheduledJobLockSupport.executeWithLock(
                "subscription_record_storage_snapshots",
                recordStorageUsageSnapshotsUseCase::execute
        );
    }

    @Scheduled(cron = "${app.subscription.renew-cron:0 */10 * * * *}")
    public void renewSubscriptions() {
        scheduledJobLockSupport.executeWithLock(
                "subscription_renew_subscriptions",
                renewSubscriptionsUseCase::execute
        );
    }

    @Scheduled(cron = "${app.subscription.process-payment-cron:0 */10 * * * *}")
    public void processPendingPayments() {
        scheduledJobLockSupport.executeWithLock(
                "subscription_process_pending_payments",
                processPendingSubscriptionPaymentsUseCase::execute
        );
    }
}
