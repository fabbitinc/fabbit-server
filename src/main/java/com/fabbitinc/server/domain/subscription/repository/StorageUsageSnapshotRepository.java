package com.fabbitinc.server.domain.subscription.repository;

import com.fabbitinc.server.domain.subscription.model.StorageUsageSnapshot;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageUsageSnapshotRepository extends JpaRepository<StorageUsageSnapshot, UUID> {

    Optional<StorageUsageSnapshot> findTopByOrgIdOrderBySnapshotAtDesc(UUID orgId);

    Optional<StorageUsageSnapshot> findByOrgIdAndSnapshotAt(UUID orgId, Instant snapshotAt);
}
