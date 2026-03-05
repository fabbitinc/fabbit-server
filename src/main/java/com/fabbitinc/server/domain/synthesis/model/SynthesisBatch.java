package com.fabbitinc.server.domain.synthesis.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.project.model.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "synthesis_batches",
        indexes = {
                @Index(name = "ix_synthesis_batches_project_id", columnList = "project_id"),
                @Index(name = "ix_synthesis_batches_mapping_id", columnList = "mapping_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SynthesisBatch extends AbstractCreatedEntity {

    public static final String CODE_SYNTHESIS_BATCH_MAPPING_REQUIRED = "SYNTHESIS_BATCH_MAPPING_REQUIRED";

    @Column(name = "project_id")
    private UUID projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(name = "mapping_id", nullable = false)
    private UUID mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_id", insertable = false, updatable = false)
    private MappingRecord mappingRecord;

    @Column(name = "requested_count", nullable = false)
    private int requestedCount;

    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failed_uploads", nullable = false, columnDefinition = "jsonb")
    private String failedUploads;

    @OneToMany(mappedBy = "batch", fetch = FetchType.LAZY)
    private List<SynthesisJob> jobs = new ArrayList<>();

    public SynthesisBatch(
            UUID projectId,
            UUID mappingId,
            int requestedCount,
            int acceptedCount,
            String failedUploads
    ) {
        super(UuidV7Generator.next());
        this.projectId = projectId;
        this.mappingId = requireMappingId(mappingId);
        this.requestedCount = requestedCount;
        this.acceptedCount = acceptedCount;
        this.failedUploads = (failedUploads == null || failedUploads.isBlank()) ? "[]" : failedUploads;
    }

    public static SynthesisBatch create(
            UUID projectId,
            UUID mappingId,
            int requestedCount,
            int acceptedCount,
            String failedUploads
    ) {
        return new SynthesisBatch(projectId, mappingId, requestedCount, acceptedCount, failedUploads);
    }

    public static SynthesisBatch create(
            Project project,
            MappingRecord mappingRecord,
            int requestedCount,
            int acceptedCount,
            String failedUploads
    ) {
        if (mappingRecord == null) {
            throw new DomainException(CODE_SYNTHESIS_BATCH_MAPPING_REQUIRED, "매핑 ID는 필수입니다");
        }
        SynthesisBatch batch = new SynthesisBatch(
                project == null ? null : project.getId(),
                mappingRecord.getId(),
                requestedCount,
                acceptedCount,
                failedUploads
        );
        batch.project = project;
        batch.mappingRecord = mappingRecord;
        return batch;
    }

    public List<SynthesisJob> getJobs() {
        return List.copyOf(jobs);
    }

    private UUID requireMappingId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_SYNTHESIS_BATCH_MAPPING_REQUIRED, "매핑 ID는 필수입니다");
        }
        return value;
    }
}
