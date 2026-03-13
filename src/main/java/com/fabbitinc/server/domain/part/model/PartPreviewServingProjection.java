package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractIdEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@AttributeOverride(name = "id", column = @Column(name = "part_preview_id", nullable = false, updatable = false))
@Table(name = "part_preview_serving_projections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartPreviewServingProjection extends AbstractIdEntity {

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

    private PartPreviewServingProjection(UUID partPreviewId) {
        super(partPreviewId);
        this.updatedAt = Instant.now();
    }

    public static PartPreviewServingProjection create(UUID partPreviewId) {
        return new PartPreviewServingProjection(partPreviewId);
    }

    public void changeServingKeys(String originalKey, String pdfKey, String glbKey, String webpKey) {
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
