package com.fabbitinc.server.domain.subscription.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "storage_usage_snapshots", schema = "public")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageUsageSnapshot extends AbstractCreatedEntity implements AggregateRoot {

    public static final String CODE_STORAGE_USAGE_SNAPSHOT_ORGANIZATION_REQUIRED = "STORAGE_USAGE_SNAPSHOT_ORGANIZATION_REQUIRED";
    public static final String CODE_STORAGE_USAGE_SNAPSHOT_AT_REQUIRED = "STORAGE_USAGE_SNAPSHOT_AT_REQUIRED";
    public static final String CODE_STORAGE_USAGE_SNAPSHOT_BYTES_INVALID = "STORAGE_USAGE_SNAPSHOT_BYTES_INVALID";

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    @Column(name = "total_file_bytes", nullable = false)
    private long totalFileBytes;

    @Column(name = "included_bytes", nullable = false)
    private long includedBytes;

    @Column(name = "billable_bytes", nullable = false)
    private long billableBytes;

    @Column(name = "overage_bytes", nullable = false)
    private long overageBytes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    private StorageUsageSnapshot(
            UUID orgId,
            Instant snapshotAt,
            long totalFileBytes,
            long includedBytes,
            long billableBytes,
            long overageBytes,
            Map<String, Object> metadata
    ) {
        super(UuidV7Generator.next());
        this.orgId = requireOrgId(orgId);
        this.snapshotAt = requireSnapshotAt(snapshotAt);
        this.totalFileBytes = requireNonNegative(totalFileBytes);
        this.includedBytes = requireNonNegative(includedBytes);
        this.billableBytes = requireNonNegative(billableBytes);
        this.overageBytes = requireNonNegative(overageBytes);
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static StorageUsageSnapshot create(
            UUID orgId,
            Instant snapshotAt,
            long totalFileBytes,
            long includedBytes,
            long billableBytes,
            long overageBytes,
            Map<String, Object> metadata
    ) {
        return new StorageUsageSnapshot(orgId, snapshotAt, totalFileBytes, includedBytes, billableBytes, overageBytes, metadata);
    }

    private UUID requireOrgId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_STORAGE_USAGE_SNAPSHOT_ORGANIZATION_REQUIRED, "조직 ID는 필수입니다");
        }
        return value;
    }

    private Instant requireSnapshotAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_STORAGE_USAGE_SNAPSHOT_AT_REQUIRED, "스냅샷 시각은 필수입니다");
        }
        return value;
    }

    private long requireNonNegative(long value) {
        if (value < 0) {
            throw new DomainException(CODE_STORAGE_USAGE_SNAPSHOT_BYTES_INVALID, "스토리지 바이트는 0 이상이어야 합니다");
        }
        return value;
    }
}
