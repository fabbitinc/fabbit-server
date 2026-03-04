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

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "mapping_id", nullable = false)
    private UUID mappingId;

    @Column(name = "requested_count", nullable = false)
    private int requestedCount;

    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount;

    @Column(name = "failed_uploads", nullable = false, columnDefinition = "jsonb")
    private String failedUploads;

    public SynthesisBatch(
            UUID projectId,
            UUID mappingId,
            int requestedCount,
            int acceptedCount,
            String failedUploads
    ) {
        super(UuidV7Generator.next());
        this.projectId = projectId;
        this.mappingId = mappingId;
        this.requestedCount = requestedCount;
        this.acceptedCount = acceptedCount;
        this.failedUploads = (failedUploads == null || failedUploads.isBlank()) ? "[]" : failedUploads;
    }
}
