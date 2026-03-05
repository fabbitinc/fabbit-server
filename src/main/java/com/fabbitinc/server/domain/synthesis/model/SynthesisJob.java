package com.fabbitinc.server.domain.synthesis.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(name = "mapping_id", nullable = false)
    private UUID mappingId;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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
        this.mappingId = mappingId;
        this.fileId = fileId;
        this.status = "PENDING";
        this.totalRows = 0;
        this.processedRows = 0;
        this.nodesCreated = 0;
        this.relationshipsCreated = 0;
        this.errors = "[]";
    }

    public void assignBatch(UUID batchId) {
        this.batchId = batchId;
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
        this.status = "PROCESSING";
        this.startedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
    }

    public void markFailed(String errors) {
        this.status = "FAILED";
        this.errors = errors == null || errors.isBlank() ? "[]" : errors;
        this.completedAt = Instant.now();
    }
}
