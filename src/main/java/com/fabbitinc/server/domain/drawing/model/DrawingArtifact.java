package com.fabbitinc.server.domain.drawing.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
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
        name = "drawing_artifacts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_drawing_artifacts_drawing_type", columnNames = {"drawing_id", "artifact_type"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrawingArtifact extends AbstractCreatedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drawing_id", nullable = false)
    private Drawing drawing;

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

    private DrawingArtifact(
            Drawing drawing,
            UUID fileId,
            DrawingArtifactType artifactType,
            String format,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        super(UuidV7Generator.next());
        this.drawing = requireDrawing(drawing);
        this.fileId = fileId;
        this.artifactType = requireArtifactType(artifactType);
        this.format = normalizeNullable(format);
        this.storageKey = requireStorageKey(storageKey);
        this.contentType = normalizeNullable(contentType);
        this.fileSize = requireFileSize(fileSize);
        this.publishedAt = Instant.now();
    }

    public static DrawingArtifact create(
            Drawing drawing,
            UUID fileId,
            DrawingArtifactType artifactType,
            String format,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        return new DrawingArtifact(drawing, fileId, artifactType, format, storageKey, contentType, fileSize);
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

    private Drawing requireDrawing(Drawing value) {
        if (value == null) {
            throw new DomainException("DRAWING_ARTIFACT_DRAWING_REQUIRED", "도면은 필수입니다");
        }
        return value;
    }

    private DrawingArtifactType requireArtifactType(DrawingArtifactType value) {
        if (value == null) {
            throw new DomainException("DRAWING_ARTIFACT_TYPE_REQUIRED", "산출물 타입은 필수입니다");
        }
        return value;
    }

    private String requireStorageKey(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new DomainException("DRAWING_ARTIFACT_KEY_REQUIRED", "산출물 키는 필수입니다");
        }
        return normalized;
    }

    private long requireFileSize(long value) {
        if (value < 0L) {
            throw new DomainException("DRAWING_ARTIFACT_FILE_SIZE_INVALID", "산출물 크기는 0 이상이어야 합니다");
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
