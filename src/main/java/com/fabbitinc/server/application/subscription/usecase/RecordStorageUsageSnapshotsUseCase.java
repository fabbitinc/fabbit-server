package com.fabbitinc.server.application.subscription.usecase;

import com.fabbitinc.server.application.subscription.api.SubscriptionApi;
import com.fabbitinc.server.application.subscription.usecase.result.RecordStorageUsageSnapshotsResult;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RecordStorageUsageSnapshotsUseCase {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionApi subscriptionApi;

    public RecordStorageUsageSnapshotsResult execute() {
        Instant snapshotAt = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        int snapshotCount = 0;

        for (Organization organization : organizationRepository.findAll()) {
            subscriptionApi.recordStorageSnapshot(organization.getId(), snapshotAt, organization.getStorageBytesUsed());
            snapshotCount++;
        }

        log.atInfo()
                .addKeyValue("event.name", "subscription.storage.snapshot.batch.completed")
                .addKeyValue("subscription.snapshotCount", snapshotCount)
                .addKeyValue("snapshot.at", snapshotAt)
                .addKeyValue("outcome", "success")
                .log("storage usage snapshots recorded");
        return new RecordStorageUsageSnapshotsResult(snapshotCount, snapshotAt);
    }
}
