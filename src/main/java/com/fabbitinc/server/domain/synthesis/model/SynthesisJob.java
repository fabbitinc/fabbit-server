package com.fabbitinc.server.domain.synthesis.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.file.model.File;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        name = "synthesis_jobs",
        indexes = {
                @Index(name = "ix_synthesis_jobs_batch_id", columnList = "batch_id"),
                @Index(name = "ix_synthesis_jobs_mapping_id", columnList = "mapping_id"),
                @Index(name = "ix_synthesis_jobs_file_id", columnList = "file_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SynthesisJob extends AbstractCreatedEntity {

    public static final String CODE_SYNTHESIS_JOB_BATCH_REQUIRED = "SYNTHESIS_JOB_BATCH_REQUIRED";
    public static final String CODE_SYNTHESIS_JOB_MAPPING_REQUIRED = "SYNTHESIS_JOB_MAPPING_REQUIRED";
    public static final String CODE_SYNTHESIS_JOB_FILE_REQUIRED = "SYNTHESIS_JOB_FILE_REQUIRED";
    public static final String CODE_SYNTHESIS_JOB_INVALID_STATE = "SYNTHESIS_JOB_INVALID_STATE";
    public static final String CODE_SYNTHESIS_JOB_TOTAL_ROWS_INVALID = "SYNTHESIS_JOB_TOTAL_ROWS_INVALID";
    public static final String CODE_SYNTHESIS_JOB_PROGRESS_INVALID = "SYNTHESIS_JOB_PROGRESS_INVALID";

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", insertable = false, updatable = false)
    private SynthesisBatch batch;

    @Column(name = "mapping_id", nullable = false)
    private UUID mappingId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "mapping_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_synthesis_jobs_mapping_id")
    )
    private MappingRecord _mappingRecordRelation;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "file_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_synthesis_jobs_file_id")
    )
    private File _fileRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SynthesisJobStatus status;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "processed_rows", nullable = false)
    private int processedRows;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "nodes_created", nullable = false)
    private int nodesCreated;

    @Column(name = "relationships_created", nullable = false)
    private int relationshipsCreated;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "errors", nullable = false, columnDefinition = "jsonb")
    private String errors;

    private SynthesisJob(SynthesisBatch batch, UUID fileId) {
        super(UuidV7Generator.next());
        this.batch = requireBatch(batch);
        this.batchId = batch.getId();
        this.mappingId = requireMappingId(batch.getMappingId());
        this.fileId = requireFileId(fileId);
        this.status = SynthesisJobStatus.PENDING;
        this.totalRows = 0;
        this.processedRows = 0;
        this.nodesCreated = 0;
        this.relationshipsCreated = 0;
        this.errors = "[]";
    }

    static SynthesisJob create(SynthesisBatch batch, UUID fileId) {
        return new SynthesisJob(batch, fileId);
    }

    public void start(int totalRows) {
        if (this.status != SynthesisJobStatus.PENDING) {
            throw new DomainException(CODE_SYNTHESIS_JOB_INVALID_STATE, "PENDING 상태에서만 PROCESSING으로 전이할 수 있습니다");
        }
        this.totalRows = requireNonNegative(totalRows, CODE_SYNTHESIS_JOB_TOTAL_ROWS_INVALID, "전체 행 수는 0 이상이어야 합니다");
        this.processedRows = 0;
        this.nodesCreated = 0;
        this.relationshipsCreated = 0;
        this.errors = "[]";
        this.status = SynthesisJobStatus.PROCESSING;
        this.startedAt = Instant.now();
        this.completedAt = null;
    }

    public void complete(int processedRows, int nodesCreated, int relationshipsCreated, String errors) {
        if (this.status != SynthesisJobStatus.PROCESSING) {
            throw new DomainException(CODE_SYNTHESIS_JOB_INVALID_STATE, "PROCESSING 상태에서만 COMPLETED로 전이할 수 있습니다");
        }
        this.processedRows = requireProgress(processedRows);
        this.nodesCreated = requireNonNegative(nodesCreated, CODE_SYNTHESIS_JOB_PROGRESS_INVALID, "생성 노드 수는 0 이상이어야 합니다");
        this.relationshipsCreated = requireNonNegative(
                relationshipsCreated,
                CODE_SYNTHESIS_JOB_PROGRESS_INVALID,
                "생성 관계 수는 0 이상이어야 합니다"
        );
        this.errors = normalizeErrors(errors);
        this.status = SynthesisJobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(String errors) {
        if (this.status != SynthesisJobStatus.PROCESSING) {
            throw new DomainException(CODE_SYNTHESIS_JOB_INVALID_STATE, "PROCESSING 상태에서만 FAILED로 전이할 수 있습니다");
        }
        this.status = SynthesisJobStatus.FAILED;
        this.errors = normalizeErrors(errors);
        this.completedAt = Instant.now();
    }

    private SynthesisBatch requireBatch(SynthesisBatch value) {
        if (value == null) {
            throw new DomainException(CODE_SYNTHESIS_JOB_BATCH_REQUIRED, "배치 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireMappingId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_SYNTHESIS_JOB_MAPPING_REQUIRED, "매핑 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireFileId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_SYNTHESIS_JOB_FILE_REQUIRED, "파일 ID는 필수입니다");
        }
        return value;
    }

    private int requireNonNegative(int value, String code, String message) {
        if (value < 0) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private int requireProgress(int value) {
        int normalized = requireNonNegative(value, CODE_SYNTHESIS_JOB_PROGRESS_INVALID, "처리 행 수는 0 이상이어야 합니다");
        if (normalized > totalRows) {
            throw new DomainException(CODE_SYNTHESIS_JOB_PROGRESS_INVALID, "처리 행 수는 전체 행 수를 초과할 수 없습니다");
        }
        return normalized;
    }

    private String normalizeErrors(String value) {
        if (value == null || value.isBlank()) {
            return "[]";
        }
        return value.trim();
    }
}
