package com.fabbitinc.server.domain.drawing.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drawing extends AbstractCreatedEntity implements AggregateRoot {

    public static final String CODE_DRAWING_NAME_REQUIRED = "DRAWING_NAME_REQUIRED";
    public static final String CODE_DRAWING_PDF_KEY_REQUIRED = "DRAWING_PDF_KEY_REQUIRED";
    public static final String CODE_DRAWING_THUMBNAIL_KEY_REQUIRED = "DRAWING_THUMBNAIL_KEY_REQUIRED";

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
    @Column(name = "conversion_status", length = 30)
    private DrawingConversionStatus conversionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private DrawingSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dimension", length = 30)
    private DrawingDimension dimension;

    @Column(name = "part_id")
    private UUID partId;

    @Column(name = "source_file_id")
    private UUID sourceFileId;

    @Column(name = "current_job_id")
    private UUID currentJobId;

    @OneToMany(mappedBy = "drawing", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DrawingArtifact> artifacts = new ArrayList<>();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private Drawing(String drawingNumber, String name) {
        super(UuidV7Generator.next());
        this.drawingNumber = normalizeNullable(drawingNumber);
        this.name = requireName(name);
        this.status = DrawingStatus.DRAFT;
        this.conversionStatus = null;
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

    public void assignPart(UUID partId) {
        if (partId == null) {
            throw new DomainException("DRAWING_PART_REQUIRED", "부품 ID는 필수입니다");
        }
        this.partId = partId;
    }

    public void unassignPart() {
        this.partId = null;
    }

    public void registerSourceFile(
            UUID sourceFileId,
            DrawingDimension dimension,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        this.dimension = dimension;
        upsertArtifact(
                DrawingArtifactType.SOURCE_ORIGINAL,
                sourceFileId,
                detectFormat(storageKey),
                storageKey,
                normalizeNullable(contentType),
                Math.max(fileSize, 0L)
        );
    }

    public void assignSourceFile(UUID sourceFileId, DrawingSourceType sourceType, DrawingDimension dimension) {
        if (sourceFileId == null) {
            throw new DomainException("DRAWING_SOURCE_FILE_REQUIRED", "도면 원본 파일 ID는 필수입니다");
        }
        this.sourceFileId = sourceFileId;
        this.sourceType = sourceType;
        this.dimension = dimension;
    }

    public void registerRenderSourceFile(
            UUID sourceFileId,
            DrawingSourceType sourceType,
            DrawingDimension dimension,
            String storageKey,
            String contentType,
            long fileSize
    ) {
        this.sourceFileId = sourceFileId;
        this.sourceType = sourceType;
        this.dimension = dimension;
        upsertArtifact(
                DrawingArtifactType.SOURCE_RENDER,
                sourceFileId,
                detectFormat(storageKey),
                storageKey,
                normalizeNullable(contentType),
                Math.max(fileSize, 0L)
        );
    }

    public void changeOriginalFileKey(String originalFileKey) {
        String normalized = normalizeNullable(originalFileKey);
        if (normalized == null) {
            removeArtifact(DrawingArtifactType.SOURCE_ORIGINAL);
            return;
        }
        upsertArtifact(
                DrawingArtifactType.SOURCE_ORIGINAL,
                null,
                detectFormat(normalized),
                normalized,
                defaultContentType(detectFormat(normalized)),
                0L
        );
    }

    public void markRenderSourceRequired() {
        this.sourceFileId = null;
        this.sourceType = null;
        this.currentJobId = null;
        this.conversionStatus = DrawingConversionStatus.ACTION_REQUIRED;
        removeArtifact(DrawingArtifactType.SOURCE_RENDER);
    }

    public void changePdfKey(String pdfKey) {
        String normalized = normalizeNullable(pdfKey);
        if (normalized == null) {
            removeArtifact(DrawingArtifactType.DERIVED_PDF);
            return;
        }
        upsertArtifact(
                DrawingArtifactType.DERIVED_PDF,
                null,
                detectFormat(normalized),
                normalized,
                "application/pdf",
                0L
        );
    }

    public void changeThumbnailKey(String thumbnailKey) {
        String normalized = normalizeNullable(thumbnailKey);
        if (normalized == null) {
            removeArtifact(DrawingArtifactType.DERIVED_WEBP);
            return;
        }
        upsertArtifact(
                DrawingArtifactType.DERIVED_WEBP,
                null,
                detectFormat(normalized),
                normalized,
                defaultContentType(detectFormat(normalized)),
                0L
        );
    }

    public void markConversionPending() {
        this.conversionStatus = DrawingConversionStatus.PENDING;
    }

    public void beginProcessing(UUID jobId) {
        if (isRenderSourceRequired()) {
            throw new DomainException(
                    "DRAWING_RENDER_SOURCE_REQUIRED",
                    "render source 업로드가 필요한 도면은 변환을 시작할 수 없습니다"
            );
        }
        this.currentJobId = jobId;
        this.conversionStatus = DrawingConversionStatus.PENDING;
    }

    public void markConversionCompleted(String pdfKey, String thumbnailKey) {
        String requiredPdfKey = requirePdfKey(pdfKey);
        String requiredThumbnailKey = requireThumbnailKey(thumbnailKey);
        upsertArtifact(
                DrawingArtifactType.DERIVED_PDF,
                null,
                detectFormat(requiredPdfKey),
                requiredPdfKey,
                "application/pdf",
                0L
        );
        upsertArtifact(
                DrawingArtifactType.DERIVED_WEBP,
                null,
                detectFormat(requiredThumbnailKey),
                requiredThumbnailKey,
                defaultContentType(detectFormat(requiredThumbnailKey)),
                0L
        );
        this.currentJobId = null;
        this.conversionStatus = DrawingConversionStatus.COMPLETED;
    }

    public void completeProcessing(UUID jobId, List<DrawingArtifactPublication> publications) {
        if (jobId != null && currentJobId != null && !currentJobId.equals(jobId)) {
            throw new DomainException("DRAWING_JOB_MISMATCH", "다른 작업 ID로 도면 처리를 완료할 수 없습니다");
        }
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
        this.conversionStatus = DrawingConversionStatus.COMPLETED;
    }

    public void markConversionFailed() {
        this.currentJobId = null;
        this.conversionStatus = DrawingConversionStatus.FAILED;
    }

    public void failProcessing(UUID jobId) {
        if (jobId != null && currentJobId != null && !currentJobId.equals(jobId)) {
            throw new DomainException("DRAWING_JOB_MISMATCH", "다른 작업 ID로 도면 처리를 실패시킬 수 없습니다");
        }
        markConversionFailed();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public String getOriginalFileKey() {
        return findArtifactKey(DrawingArtifactType.SOURCE_ORIGINAL);
    }

    public String getPdfKey() {
        return findArtifactKey(DrawingArtifactType.DERIVED_PDF);
    }

    public String getRenderSourceFileKey() {
        String renderSourceKey = findArtifactKey(DrawingArtifactType.SOURCE_RENDER);
        if (renderSourceKey != null) {
            return renderSourceKey;
        }
        return sourceFileId == null ? null : getOriginalFileKey();
    }

    public String getThumbnailKey() {
        return findArtifactKey(DrawingArtifactType.DERIVED_WEBP);
    }

    public String getWebpKey() {
        return findArtifactKey(DrawingArtifactType.DERIVED_WEBP);
    }

    public String getGlbKey() {
        return findArtifactKey(DrawingArtifactType.DERIVED_GLB);
    }

    public String getOriginalFormat() {
        return findArtifactFormat(DrawingArtifactType.SOURCE_ORIGINAL);
    }

    public DrawingRenderSourceGroup getExpectedRenderSourceGroup() {
        return DrawingExtension.fromFormat(getOriginalFormat())
                .map(DrawingExtension::getRequiredRenderSourceGroup)
                .orElse(null);
    }

    public boolean isRenderSourceRequired() {
        return sourceFileId == null && getExpectedRenderSourceGroup() != null;
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

    private void removeArtifact(DrawingArtifactType artifactType) {
        DrawingArtifact existing = findArtifact(artifactType);
        if (existing != null) {
            artifacts.remove(existing);
        }
    }

    private DrawingArtifact findArtifact(DrawingArtifactType artifactType) {
        return artifacts.stream()
                .filter(artifact -> artifact.getArtifactType() == artifactType)
                .findFirst()
                .orElse(null);
    }

    private String findArtifactKey(DrawingArtifactType artifactType) {
        DrawingArtifact artifact = findArtifact(artifactType);
        return artifact == null ? null : artifact.getStorageKey();
    }

    private String findArtifactFormat(DrawingArtifactType artifactType) {
        DrawingArtifact artifact = findArtifact(artifactType);
        return artifact == null ? null : artifact.getFormat();
    }

    private String requireName(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new DomainException(CODE_DRAWING_NAME_REQUIRED, "도면명은 필수입니다");
        }
        return normalized;
    }

    private String requirePdfKey(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new DomainException(CODE_DRAWING_PDF_KEY_REQUIRED, "PDF 키는 필수입니다");
        }
        return normalized;
    }

    private String requireThumbnailKey(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new DomainException(CODE_DRAWING_THUMBNAIL_KEY_REQUIRED, "썸네일 키는 필수입니다");
        }
        return normalized;
    }

    private String requireStorageKey(String value, DrawingArtifactType artifactType) {
        String normalized = normalizeNullable(value);
        if (normalized != null) {
            return normalized;
        }
        if (artifactType == DrawingArtifactType.DERIVED_PDF) {
            throw new DomainException(CODE_DRAWING_PDF_KEY_REQUIRED, "PDF 키는 필수입니다");
        }
        if (artifactType == DrawingArtifactType.DERIVED_WEBP) {
            throw new DomainException(CODE_DRAWING_THUMBNAIL_KEY_REQUIRED, "썸네일 키는 필수입니다");
        }
        throw new DomainException("DRAWING_ARTIFACT_KEY_REQUIRED", "산출물 키는 필수입니다");
    }

    private String detectFormat(String storageKey) {
        String normalized = normalizeNullable(storageKey);
        if (normalized == null) {
            return null;
        }
        int idx = normalized.lastIndexOf('.');
        if (idx < 0 || idx >= normalized.length() - 1) {
            return null;
        }
        return normalized.substring(idx + 1).trim().toLowerCase();
    }

    private String defaultContentType(String format) {
        if (format == null) {
            return "application/octet-stream";
        }
        return switch (format) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "glb" -> "model/gltf-binary";
            default -> "application/octet-stream";
        };
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
