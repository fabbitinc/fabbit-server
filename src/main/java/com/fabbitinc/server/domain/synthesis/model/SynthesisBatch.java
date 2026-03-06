package com.fabbitinc.server.domain.synthesis.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import com.fabbitinc.server.domain.project.model.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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
public class SynthesisBatch extends AbstractCreatedEntity implements AggregateRoot {

    public static final String CODE_SYNTHESIS_BATCH_MAPPING_REQUIRED = "SYNTHESIS_BATCH_MAPPING_REQUIRED";
    public static final String CODE_SYNTHESIS_BATCH_REQUESTED_COUNT_INVALID = "SYNTHESIS_BATCH_REQUESTED_COUNT_INVALID";
    public static final String CODE_SYNTHESIS_BATCH_ACCEPTED_COUNT_INVALID = "SYNTHESIS_BATCH_ACCEPTED_COUNT_INVALID";

    @Column(name = "project_id")
    private UUID projectId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "project_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_synthesis_batches_project_id")
    )
    private Project _projectRelation;

    @Column(name = "mapping_id", nullable = false)
    private UUID mappingId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "mapping_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_synthesis_batches_mapping_id")
    )
    private MappingRecord _mappingRecordRelation;

    @Column(name = "requested_count", nullable = false)
    private int requestedCount;

    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failed_uploads", nullable = false, columnDefinition = "jsonb")
    private String failedUploads;

    @OneToMany(mappedBy = "batch", fetch = FetchType.LAZY)
    private List<SynthesisJob> jobs = new ArrayList<>();

    private SynthesisBatch(
            UUID projectId,
            UUID mappingId,
            int requestedCount,
            String failedUploads
    ) {
        super(UuidV7Generator.next());
        this.projectId = projectId;
        this.mappingId = requireMappingId(mappingId);
        this.requestedCount = requireRequestedCount(requestedCount);
        this.acceptedCount = 0;
        this.failedUploads = normalizeFailedUploads(failedUploads);
    }

    public static SynthesisBatch create(
            UUID projectId,
            UUID mappingId,
            int requestedCount,
            String failedUploads
    ) {
        return new SynthesisBatch(projectId, mappingId, requestedCount, failedUploads);
    }

    public SynthesisJob addJob(UUID fileId) {
        if (acceptedCount + 1 > requestedCount) {
            throw new DomainException(
                    CODE_SYNTHESIS_BATCH_ACCEPTED_COUNT_INVALID,
                    "수락 건수는 요청 건수를 초과할 수 없습니다"
            );
        }
        SynthesisJob job = SynthesisJob.create(this, fileId);
        jobs.add(job);
        acceptedCount = jobs.size();
        return job;
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

    private int requireRequestedCount(int value) {
        if (value < 0) {
            throw new DomainException(CODE_SYNTHESIS_BATCH_REQUESTED_COUNT_INVALID, "요청 건수는 0 이상이어야 합니다");
        }
        return value;
    }

    private int requireAcceptedCount(int acceptedCount, int requestedCount) {
        if (acceptedCount < 0 || acceptedCount > requestedCount) {
            throw new DomainException(
                    CODE_SYNTHESIS_BATCH_ACCEPTED_COUNT_INVALID,
                    "수락 건수는 0 이상이며 요청 건수 이하여야 합니다"
            );
        }
        return acceptedCount;
    }

    private String normalizeFailedUploads(String value) {
        if (value == null || value.isBlank()) {
            return "[]";
        }
        return value.trim();
    }
}
