package com.fabbitinc.server.domain.mappingv2.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.file.model.File;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "mapping_v2_revisions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_mapping_v2_revisions_record_version",
                        columnNames = {"record_id", "version"}
                )
        },
        indexes = {
                @Index(name = "ix_mapping_v2_revisions_record_id", columnList = "record_id"),
                @Index(name = "ix_mapping_v2_revisions_file_id", columnList = "file_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MappingV2Revision extends AbstractCreatedEntity {

    public static final String CODE_MAPPING_V2_REVISION_RECORD_REQUIRED = "MAPPING_V2_REVISION_RECORD_REQUIRED";
    public static final String CODE_MAPPING_V2_REVISION_FILE_REQUIRED = "MAPPING_V2_REVISION_FILE_REQUIRED";
    public static final String CODE_MAPPING_V2_REVISION_VERSION_INVALID = "MAPPING_V2_REVISION_VERSION_INVALID";
    public static final String CODE_MAPPING_V2_REVISION_SHEET_NAME_TOO_LONG =
            "MAPPING_V2_REVISION_SHEET_NAME_TOO_LONG";
    public static final String CODE_MAPPING_V2_REVISION_USAGE_INCREMENT_INVALID =
            "MAPPING_V2_REVISION_USAGE_INCREMENT_INVALID";

    private static final int MAX_SHEET_NAME_LENGTH = 200;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "record_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_mapping_v2_revisions_record_id")
    )
    private MappingV2Record record;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "file_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_mapping_v2_revisions_file_id")
    )
    private File _fileRelation;

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

    private MappingV2Revision(
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
        this.version = requireVersion(version);
        this.sheetName = normalizeSheetName(sheetName);
        this.originalHeaders = normalizeJson(originalHeaders);
        this.mapping = normalizeJson(mapping);
        this.usageCount = 0;
    }

    static MappingV2Revision create(
            MappingV2Record record,
            UUID fileId,
            int version,
            String sheetName,
            String originalHeaders,
            String mapping
    ) {
        if (record == null) {
            throw new DomainException(
                    CODE_MAPPING_V2_REVISION_RECORD_REQUIRED,
                    "매핑 레코드 ID는 필수입니다"
            );
        }
        if (fileId == null) {
            throw new DomainException(CODE_MAPPING_V2_REVISION_FILE_REQUIRED, "파일 ID는 필수입니다");
        }
        MappingV2Revision revision = new MappingV2Revision(
                record.getId(),
                fileId,
                version,
                sheetName,
                originalHeaders,
                mapping
        );
        revision.record = record;
        return revision;
    }

    public void incrementUsage(int amount) {
        if (amount <= 0) {
            throw new DomainException(
                    CODE_MAPPING_V2_REVISION_USAGE_INCREMENT_INVALID,
                    "사용량 증가는 1 이상이어야 합니다"
            );
        }
        this.usageCount += amount;
    }

    private String normalizeJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        return raw.trim();
    }

    private int requireVersion(int value) {
        if (value <= 0) {
            throw new DomainException(CODE_MAPPING_V2_REVISION_VERSION_INVALID, "리비전 버전은 1 이상이어야 합니다");
        }
        return value;
    }

    private String normalizeSheetName(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            return null;
        }
        if (trimmed.length() > MAX_SHEET_NAME_LENGTH) {
            throw new DomainException(
                    CODE_MAPPING_V2_REVISION_SHEET_NAME_TOO_LONG,
                    "시트명은 200자 이하여야 합니다"
            );
        }
        return trimmed;
    }

    private UUID requireRecordId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_MAPPING_V2_REVISION_RECORD_REQUIRED, "매핑 레코드 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireFileId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_MAPPING_V2_REVISION_FILE_REQUIRED, "파일 ID는 필수입니다");
        }
        return value;
    }
}
