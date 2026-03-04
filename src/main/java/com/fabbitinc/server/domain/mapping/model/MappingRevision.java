package com.fabbitinc.server.domain.mapping.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "mapping_revisions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_mapping_revisions_record_version",
                        columnNames = {"record_id", "version"}
                )
        },
        indexes = {
                @Index(name = "ix_mapping_revisions_record_id", columnList = "record_id"),
                @Index(name = "ix_mapping_revisions_file_id", columnList = "file_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MappingRevision extends AbstractCreatedEntity {

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "sheet_name", length = 200)
    private String sheetName;

    @Column(name = "original_headers", nullable = false, columnDefinition = "jsonb")
    private String originalHeaders;

    @Column(name = "mapping", nullable = false, columnDefinition = "jsonb")
    private String mapping;

    @Column(name = "usage_count", nullable = false)
    private int usageCount;

    public MappingRevision(
            UUID recordId,
            UUID fileId,
            int version,
            String sheetName,
            String originalHeaders,
            String mapping
    ) {
        super(UuidV7Generator.next());
        this.recordId = recordId;
        this.fileId = fileId;
        this.version = version;
        this.sheetName = sheetName;
        this.originalHeaders = normalizeJson(originalHeaders);
        this.mapping = normalizeJson(mapping);
        this.usageCount = 0;
    }

    public void incrementUsage(int amount) {
        this.usageCount += amount;
    }

    private String normalizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        return raw;
    }
}
