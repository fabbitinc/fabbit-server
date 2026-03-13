package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "part_preview_artifacts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_part_preview_artifacts_preview_type",
                        columnNames = {"part_preview_id", "artifact_type"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartPreviewArtifact extends AbstractCreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_preview_id", nullable = false)
    private PartPreview partPreview;

    @Column(name = "file_id")
    private UUID fileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 50)
    private DrawingArtifactType artifactType;

    @Column(name = "format", length = 30)
    private String format;

    @Column(name = "storage_key", nullable = false, length = 1000)
    private String storageKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    private PartPreviewArtifact(
            PartPreview partPreview,
            UUID fileId,
            DrawingArtifactType artifactType,
            String format,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        super(UuidV7Generator.next());
        this.partPreview = requirePartPreview(partPreview);
        this.fileId = fileId;
        this.artifactType = requireArtifactType(artifactType);
        this.format = normalizeNullable(format);
        this.storageKey = requireStorageKey(storageKey);
        this.contentType = normalizeNullable(contentType);
        this.fileSize = requireFileSize(fileSize);
        this.publishedAt = Instant.now();
    }

    public static PartPreviewArtifact create(
            PartPreview partPreview,
            UUID fileId,
            DrawingArtifactType artifactType,
            String format,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        return new PartPreviewArtifact(partPreview, fileId, artifactType, format, storageKey, contentType, fileSize);
    }

    public void changeStoredObject(
            UUID fileId,
            String format,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        this.fileId = fileId;
        this.format = normalizeNullable(format);
        this.storageKey = requireStorageKey(storageKey);
        this.contentType = normalizeNullable(contentType);
        this.fileSize = requireFileSize(fileSize);
        this.publishedAt = Instant.now();
    }

    private PartPreview requirePartPreview(PartPreview value) {
        if (value == null) {
            throw new DomainException("PART_PREVIEW_ARTIFACT_PREVIEW_REQUIRED", "대표 미리보기는 필수입니다");
        }
        return value;
    }

    private DrawingArtifactType requireArtifactType(DrawingArtifactType value) {
        if (value == null) {
            throw new DomainException("PART_PREVIEW_ARTIFACT_TYPE_REQUIRED", "대표 미리보기 산출물 타입은 필수입니다");
        }
        return value;
    }

    private String requireStorageKey(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new DomainException("PART_PREVIEW_ARTIFACT_KEY_REQUIRED", "대표 미리보기 산출물 키는 필수입니다");
        }
        return normalized;
    }

    private long requireFileSize(long value) {
        if (value < 0L) {
            throw new DomainException("PART_PREVIEW_ARTIFACT_FILE_SIZE_INVALID", "대표 미리보기 산출물 크기는 0 이상이어야 합니다");
        }
        return value;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
