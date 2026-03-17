package com.fabbitinc.server.domain.drawing.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.part.model.PartRevision;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "drawings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_drawings_drawing_number", columnNames = "drawing_number")
        },
        indexes = {
                @Index(name = "ix_drawings_part_revision_id", columnList = "part_revision_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drawing extends AbstractCreatedEntity implements AggregateRoot {

    public static final String CODE_DRAWING_NAME_REQUIRED = "DRAWING_NAME_REQUIRED";

    @Column(name = "drawing_number", length = 100)
    private String drawingNumber;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "version", length = 50)
    private String version;

    @Convert(converter = DrawingStatusConverter.class)
    @Column(name = "status", length = 50)
    private DrawingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private DrawingSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", length = 30)
    private DrawingDimension dimension;

    @Column(name = "part_revision_id")
    private UUID partRevisionId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_revision_id", insertable = false, updatable = false)
    private PartRevision _partRevisionRelation;

    @Column(name = "source_file_id")
    private UUID sourceFileId;

    @Column(name = "original_file_key", length = 1000)
    private String originalFileKey;

    @OneToMany(mappedBy = "drawing", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DrawingArtifact> artifacts = new ArrayList<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Drawing(String drawingNumber, String name) {
        super(UuidV7Generator.next());
        this.drawingNumber = normalizeNullable(drawingNumber);
        this.name = requireName(name);
        this.status = DrawingStatus.DRAFT;
    }

    public static Drawing create(String drawingNumber, String name) {
        return new Drawing(drawingNumber, name);
    }

    public void changeDrawingNumber(String drawingNumber) {
        this.drawingNumber = normalizeNullable(drawingNumber);
    }

    public void changeName(String name) {
        this.name = requireName(name);
    }

    public void changeVersion(String version) {
        this.version = normalizeNullable(version);
    }

    public void changeStatus(DrawingStatus status) {
        this.status = status;
    }

    public void assignPartRevision(UUID partRevisionId) {
        if (partRevisionId == null) {
            throw new DomainException("DRAWING_PART_REVISION_REQUIRED", "부품 리비전 ID는 필수입니다");
        }
        this.partRevisionId = partRevisionId;
    }

    public void unassignPartRevision() {
        this.partRevisionId = null;
    }

    public void assignSourceFile(UUID sourceFileId, DrawingSourceType sourceType, DrawingDimension dimension) {
        if (sourceFileId == null) {
            throw new DomainException("DRAWING_SOURCE_FILE_REQUIRED", "도면 원본 파일 ID는 필수입니다");
        }
        this.sourceFileId = sourceFileId;
        this.sourceType = sourceType;
        this.dimension = dimension;
    }

    public void changeOriginalFileKey(String originalFileKey) {
        this.originalFileKey = normalizeNullable(originalFileKey);
    }

    public void softDelete(UUID actorId) {
        this.deletedAt = Instant.now();
    }

    public String getOriginalFileKey() {
        return originalFileKey;
    }

    public List<DrawingArtifact> getArtifacts() {
        return List.copyOf(artifacts);
    }

    private void upsertArtifact(
            DrawingArtifactType artifactType,
            UUID fileId,
            String format,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        String requiredStorageKey = requireStorageKey(storageKey, artifactType);
        DrawingArtifact existing = findArtifact(artifactType);
        if (existing == null) {
            artifacts.add(DrawingArtifact.create(
                    this,
                    fileId,
                    artifactType,
                    normalizeNullable(format),
                    requiredStorageKey,
                    normalizeNullable(contentType),
                    Math.max(fileSize, 0L)
            ));
            return;
        }
        existing.changeStoredObject(
                fileId,
                normalizeNullable(format),
                requiredStorageKey,
                normalizeNullable(contentType),
                Math.max(fileSize, 0L)
        );
    }

    private DrawingArtifact findArtifact(DrawingArtifactType artifactType) {
        return artifacts.stream()
                .filter(artifact -> artifact.getArtifactType() == artifactType)
                .findFirst()
                .orElse(null);
    }

    private String requireName(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new DomainException(CODE_DRAWING_NAME_REQUIRED, "도면명은 필수입니다");
        }
        return normalized;
    }

    private String requireStorageKey(String value, DrawingArtifactType artifactType) {
        String normalized = normalizeNullable(value);
        if (normalized != null) {
            return normalized;
        }
        throw new DomainException("DRAWING_ARTIFACT_KEY_REQUIRED", "산출물 키는 필수입니다");
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
