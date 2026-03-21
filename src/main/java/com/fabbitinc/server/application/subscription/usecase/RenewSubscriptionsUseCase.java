package com.fabbitinc.server.application.subscription.usecase;

import com.fabbitinc.server.application.subscription.service.SubscriptionService;
import com.fabbitinc.server.application.subscription.usecase.result.RenewSubscriptionsResult;
import com.fabbitinc.server.application.tenant.support.TenantContextHolder;
import com.fabbitinc.server.application.tenant.support.TenantSchemaPolicy;
import com.fabbitinc.server.domain.subscription.model.Subscription;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RenewSubscriptionsUseCase {

    private final SubscriptionService subscriptionService;
    private final TransactionTemplate transactionTemplate;

    public RenewSubscriptionsResult execute() {
        Instant renewedAt = Instant.now();
        List<Subscription> dueSubscriptions = subscriptionService.getDueSubscriptions(renewedAt);

        int renewedCount = 0;
        int canceledCount = 0;
        int failedCount = 0;

        for (Subscription subscription : dueSubscriptions) {
            try {
                TenantContextHolder.setCurrentSchema(TenantSchemaPolicy.schemaNameForOrgId(subscription.getOrgId()));
                SubscriptionService.SubscriptionRenewalResult result = transactionTemplate.execute(status ->
                        subscriptionService.renewSubscription(subscription.getOrgId(), renewedAt)
                );
                if (result == null) {
                    throw new IllegalStateException("구독 갱신 결과가 비어 있습니다");
                }
                renewedCount += result.renewed() ? 1 : 0;
                canceledCount += result.canceled() ? 1 : 0;
            } catch (Exception ex) {
                failedCount++;
                log.warn(
                        "event=subscription_renewal_failed org_id={} reason={}",
                        subscription.getOrgId(),
                        ex.getMessage(),
                        ex
                );
            } finally {
                TenantContextHolder.clear();
            }
        }

        var logEvent = log.atInfo();
        if (renewedCount == 0 && canceledCount == 0 && failedCount == 0) {
            logEvent = log.atDebug();
        }
        logEvent
                .addKeyValue("event.name", "subscription.renewal.batch.completed")
                .addKeyValue("subscription.renewedCount", renewedCount)
                .addKeyValue("subscription.canceledCount", canceledCount)
                .addKeyValue("subscription.failedCount", failedCount)
                .addKeyValue("outcome", "success")
                .log("subscriptions renewed");
        return new RenewSubscriptionsResult(renewedCount, canceledCount, failedCount);
    }
}
