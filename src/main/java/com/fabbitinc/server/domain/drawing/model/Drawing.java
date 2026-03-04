package com.fabbitinc.server.domain.drawing.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "drawing_number", length = 100)
    private String drawingNumber;

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "version", length = 50)
    private String version;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "conversion_status", length = 30)
    private String conversionStatus;

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
        this.drawingNumber = drawingNumber;
        this.name = name;
    }

    public void setOriginalFileKey(String originalFileKey) {
        this.originalFileKey = originalFileKey;
    }

    public void setPdfKey(String pdfKey) {
        this.pdfKey = pdfKey;
    }

    public void setThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
    }

    public void markConversionPending() {
        this.conversionStatus = "PENDING";
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }
}
