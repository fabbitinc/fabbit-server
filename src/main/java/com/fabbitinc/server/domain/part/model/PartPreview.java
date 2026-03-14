package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactPublication;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "part_previews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_part_previews_part_revision_id", columnNames = "part_revision_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartPreview extends AbstractCreatedEntity implements AggregateRoot {

    @Column(name = "part_revision_id", nullable = false)
    private UUID partRevisionId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_revision_id", insertable = false, updatable = false)
    private PartRevision _partRevisionRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20)
    private PartPreviewSourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversion_status", length = 30)
    private PartPreviewProcessingStatus processingStatus;

    @Column(name = "current_job_id")
    private UUID currentJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", length = 30)
    private DrawingDimension dimension;

    @OneToMany(mappedBy = "partPreview", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartPreviewArtifact> artifacts = new ArrayList<>();

    private PartPreview(UUID partRevisionId) {
        super(UuidV7Generator.next());
        this.partRevisionId = requirePartRevisionId(partRevisionId);
    }

    public static PartPreview create(UUID partRevisionId) {
        return new PartPreview(partRevisionId);
    }

    public void replaceSource(PartPreviewSourceType sourceType, UUID sourceId, DrawingDimension dimension) {
        this.sourceType = requireSourceType(sourceType);
        this.sourceId = requireSourceId(sourceId);
        this.dimension = requireDimension(dimension);
        this.currentJobId = null;
        this.processingStatus = PartPreviewProcessingStatus.PENDING;
        artifacts.clear();
    }

    public void registerSourceFile(
            UUID sourceFileId,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        upsertArtifact(
                DrawingArtifactType.SOURCE_ORIGINAL,
                sourceFileId,
                detectFormat(storageKey),
                storageKey,
                normalizeNullable(contentType),
                Math.max(fileSize, 0L)
        );
    }

    public void clearSource() {
        this.sourceType = null;
        this.sourceId = null;
        this.dimension = null;
        this.currentJobId = null;
        this.processingStatus = null;
        artifacts.clear();
    }

    public boolean hasSource() {
        return sourceType != null && sourceId != null;
    }

    public void beginProcessing(UUID jobId) {
        if (!hasSource()) {
            throw new DomainException("PART_PREVIEW_SOURCE_REQUIRED", "대표 미리보기 소스는 필수입니다");
        }
        if (jobId == null) {
            throw new DomainException("PART_PREVIEW_JOB_REQUIRED", "미리보기 작업 ID는 필수입니다");
        }
        this.currentJobId = jobId;
        this.processingStatus = PartPreviewProcessingStatus.PENDING;
    }

    public void markProcessing(UUID jobId) {
        validateCurrentJob(jobId);
        if (!hasSource()) {
            throw new DomainException("PART_PREVIEW_SOURCE_REQUIRED", "대표 미리보기 소스는 필수입니다");
        }
        this.currentJobId = jobId;
        this.processingStatus = PartPreviewProcessingStatus.PROCESSING;
    }

    public void completeProcessing(UUID jobId, List<DrawingArtifactPublication> publications) {
        validateCurrentJob(jobId);
        for (DrawingArtifactPublication publication : publications) {
            upsertArtifact(
                    publication.artifactType(),
                    publication.fileId(),
                    publication.format(),
                    publication.storageKey(),
                    publication.contentType(),
                    publication.fileSize()
            );
        }
        this.currentJobId = null;
        this.processingStatus = PartPreviewProcessingStatus.COMPLETED;
    }

    public void failProcessing(UUID jobId) {
        validateCurrentJob(jobId);
        this.currentJobId = null;
        this.processingStatus = PartPreviewProcessingStatus.FAILED;
    }

    public List<PartPreviewArtifact> getArtifacts() {
        return List.copyOf(artifacts);
    }

    public String getOriginalFileKey() {
        return findArtifactKey(DrawingArtifactType.SOURCE_ORIGINAL);
    }

    public String getPdfKey() {
        return findArtifactKey(DrawingArtifactType.DERIVED_PDF);
    }

    public String getWebpKey() {
        return findArtifactKey(DrawingArtifactType.DERIVED_WEBP);
    }

    public String getGlbKey() {
        return findArtifactKey(DrawingArtifactType.DERIVED_GLB);
    }

    private void validateCurrentJob(UUID jobId) {
        if (jobId != null && currentJobId != null && !currentJobId.equals(jobId)) {
            throw new DomainException("PART_PREVIEW_JOB_MISMATCH", "다른 작업 ID로 대표 미리보기를 처리할 수 없습니다");
        }
    }

    private void upsertArtifact(
            DrawingArtifactType artifactType,
            UUID fileId,
            String format,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        PartPreviewArtifact existing = findArtifact(artifactType);
        if (existing == null) {
            artifacts.add(PartPreviewArtifact.create(
                    this,
                    fileId,
                    artifactType,
                    format,
                    storageKey,
                    contentType,
                    fileSize
            ));
            return;
        }
        existing.changeStoredObject(fileId, format, storageKey, contentType, fileSize);
    }

    private PartPreviewArtifact findArtifact(DrawingArtifactType artifactType) {
        return artifacts.stream()
                .filter(artifact -> artifact.getArtifactType() == artifactType)
                .findFirst()
                .orElse(null);
    }

    private String findArtifactKey(DrawingArtifactType artifactType) {
        PartPreviewArtifact artifact = findArtifact(artifactType);
        return artifact == null ? null : artifact.getStorageKey();
    }

    private String detectFormat(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int idx = value.lastIndexOf('.');
        if (idx < 0 || idx >= value.length() - 1) {
            return null;
        }
        return value.substring(idx + 1).trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID requirePartRevisionId(UUID value) {
        if (value == null) {
            throw new DomainException("PART_PREVIEW_REVISION_REQUIRED", "부품 리비전 ID는 필수입니다");
        }
        return value;
    }

    private PartPreviewSourceType requireSourceType(PartPreviewSourceType value) {
        if (value == null) {
            throw new DomainException("PART_PREVIEW_SOURCE_TYPE_REQUIRED", "대표 미리보기 소스 타입은 필수입니다");
        }
        return value;
    }

    private UUID requireSourceId(UUID value) {
        if (value == null) {
            throw new DomainException("PART_PREVIEW_SOURCE_ID_REQUIRED", "대표 미리보기 소스 ID는 필수입니다");
        }
        return value;
    }

    private DrawingDimension requireDimension(DrawingDimension value) {
        if (value == null) {
            throw new DomainException("PART_PREVIEW_DIMENSION_REQUIRED", "대표 미리보기 차원은 필수입니다");
        }
        return value;
    }
}
