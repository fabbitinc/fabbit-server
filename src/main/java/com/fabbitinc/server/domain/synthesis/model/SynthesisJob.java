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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "batch_id")
    private UUID batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", insertable = false, updatable = false)
    private SynthesisBatch batch;

    @Column(name = "mapping_id", nullable = false)
    private UUID mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_id", insertable = false, updatable = false)
    private MappingRecord mappingRecord;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", insertable = false, updatable = false)
    private File file;

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

    public SynthesisJob(UUID mappingId, UUID fileId) {
        super(UuidV7Generator.next());
        this.mappingId = requireMappingId(mappingId);
        this.fileId = requireFileId(fileId);
        this.status = SynthesisJobStatus.PENDING;
        this.totalRows = 0;
        this.processedRows = 0;
        this.nodesCreated = 0;
        this.relationshipsCreated = 0;
        this.errors = "[]";
    }

    public void assignBatch(UUID batchId) {
        this.batchId = requireBatchId(batchId);
        if (this.batch != null && !batchId.equals(this.batch.getId())) {
            this.batch = null;
        }
    }

    public static SynthesisJob create(UUID mappingId, UUID fileId) {
        return new SynthesisJob(mappingId, fileId);
    }

    public static SynthesisJob create(MappingRecord mappingRecord, File file) {
        if (mappingRecord == null) {
            throw new DomainException(CODE_SYNTHESIS_JOB_MAPPING_REQUIRED, "매핑 ID는 필수입니다");
        }
        if (file == null) {
            throw new DomainException(CODE_SYNTHESIS_JOB_FILE_REQUIRED, "파일 ID는 필수입니다");
        }
        SynthesisJob job = new SynthesisJob(mappingRecord.getId(), file.getId());
        job.mappingRecord = mappingRecord;
        job.file = file;
        return job;
    }

    public void assignBatch(SynthesisBatch batch) {
        if (batch == null) {
            throw new DomainException(CODE_SYNTHESIS_JOB_BATCH_REQUIRED, "배치 ID는 필수입니다");
        }
        this.batch = batch;
        this.batchId = batch.getId();
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = Math.max(totalRows, 0);
    }

    public void incrementUsageProgress(int processedRows, int nodesCreated, int relationshipsCreated) {
        this.processedRows = processedRows;
        this.nodesCreated = nodesCreated;
        this.relationshipsCreated = relationshipsCreated;
    }

    public void replaceErrors(String errors) {
        this.errors = errors == null || errors.isBlank() ? "[]" : errors;
    }

    public void markProcessing() {
        this.status = SynthesisJobStatus.PROCESSING;
        this.startedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = SynthesisJobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errors) {
        this.status = SynthesisJobStatus.FAILED;
        this.errors = errors == null || errors.isBlank() ? "[]" : errors;
        this.completedAt = Instant.now();
    }

    private UUID requireBatchId(UUID value) {
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
}
