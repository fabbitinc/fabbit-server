package com.fabbitinc.server.domain.drawing.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "drawings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_drawings_drawing_number", columnNames = "drawing_number")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Drawing extends AbstractCreatedEntity {

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
    @Column(name = "conversion_status", length = 30)
    private DrawingConversionStatus conversionStatus;

    @Column(name = "thumbnail_key", length = 1000)
    private String thumbnailKey;

    @Column(name = "pdf_key", length = 1000)
    private String pdfKey;

    @Column(name = "original_file_key", length = 1000)
    private String originalFileKey;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Drawing(String drawingNumber, String name) {
        super(UuidV7Generator.next());
        this.drawingNumber = normalizeNullable(drawingNumber);
        this.name = requireName(name);
    }

    public static Drawing create(String drawingNumber, String name) {
        return new Drawing(drawingNumber, name);
    }

    public void setOriginalFileKey(String originalFileKey) {
        this.originalFileKey = normalizeNullable(originalFileKey);
    }

    public void setPdfKey(String pdfKey) {
        this.pdfKey = normalizeNullable(pdfKey);
    }

    public void setThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = normalizeNullable(thumbnailKey);
    }

    public void markConversionPending() {
        this.conversionStatus = DrawingConversionStatus.PENDING;
    }

    public void markConversionCompleted(String pdfKey, String thumbnailKey) {
        this.pdfKey = normalizeNullable(pdfKey);
        this.thumbnailKey = normalizeNullable(thumbnailKey);
        this.conversionStatus = DrawingConversionStatus.COMPLETED;
    }

    public void markConversionFailed() {
        this.conversionStatus = DrawingConversionStatus.FAILED;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    private String requireName(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new DomainException(CODE_DRAWING_NAME_REQUIRED, "도면명은 필수입니다");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
