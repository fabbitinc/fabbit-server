package com.fabbitinc.server.domain.drawing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "drawing_serving_projections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrawingServingProjection {

    @Id
    @Column(name = "drawing_id", nullable = false)
    private UUID drawingId;

    @Column(name = "original_key", length = 1000)
    private String originalKey;

    @Column(name = "pdf_key", length = 1000)
    private String pdfKey;

    @Column(name = "glb_key", length = 1000)
    private String glbKey;

    @Column(name = "webp_key", length = 1000)
    private String webpKey;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private DrawingServingProjection(UUID drawingId) {
        this.drawingId = drawingId;
        this.updatedAt = Instant.now();
    }

    public static DrawingServingProjection create(UUID drawingId) {
        return new DrawingServingProjection(drawingId);
    }

    public void changeServingKeys(
            String originalKey,
            String pdfKey,
            String glbKey,
            String webpKey
    ) {
        this.originalKey = normalizeNullable(originalKey);
        this.pdfKey = normalizeNullable(pdfKey);
        this.glbKey = normalizeNullable(glbKey);
        this.webpKey = normalizeNullable(webpKey);
        this.updatedAt = Instant.now();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
