package com.fabbitinc.server.domain.mapping.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.file.model.File;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    public static final String CODE_MAPPING_REVISION_RECORD_REQUIRED = "MAPPING_REVISION_RECORD_REQUIRED";
    public static final String CODE_MAPPING_REVISION_FILE_REQUIRED = "MAPPING_REVISION_FILE_REQUIRED";

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", insertable = false, updatable = false)
    private MappingRecord record;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", insertable = false, updatable = false)
    private File file;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "sheet_name", length = 200)
    private String sheetName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_headers", nullable = false, columnDefinition = "jsonb")
    private String originalHeaders;

    @JdbcTypeCode(SqlTypes.JSON)
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
        this.recordId = requireRecordId(recordId);
        this.fileId = requireFileId(fileId);
        this.version = version;
        this.sheetName = sheetName;
        this.originalHeaders = normalizeJson(originalHeaders);
        this.mapping = normalizeJson(mapping);
        this.usageCount = 0;
    }

    public static MappingRevision create(
            MappingRecord record,
            File file,
            int version,
            String sheetName,
            String originalHeaders,
            String mapping
    ) {
        if (record == null) {
            throw new DomainException(CODE_MAPPING_REVISION_RECORD_REQUIRED, "매핑 레코드 ID는 필수입니다");
        }
        if (file == null) {
            throw new DomainException(CODE_MAPPING_REVISION_FILE_REQUIRED, "파일 ID는 필수입니다");
        }
        MappingRevision revision = new MappingRevision(
                record.getId(),
                file.getId(),
                version,
                sheetName,
                originalHeaders,
                mapping
        );
        revision.record = record;
        revision.file = file;
        record.addRevision(revision);
        return revision;
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

    private UUID requireRecordId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_MAPPING_REVISION_RECORD_REQUIRED, "매핑 레코드 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireFileId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_MAPPING_REVISION_FILE_REQUIRED, "파일 ID는 필수입니다");
        }
        return value;
    }
}
