package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fabbitinc.server.domain.drawing.model.DrawingArtifactPublication;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartPreviewTest {

    @Test
    void replaceSource_기존_SOURCE_ORIGINAL을_재사용하고_파생_산출물만_제거할_수_있다() {
        PartPreview preview = PartPreview.create(UUID.randomUUID());
        preview.replaceSource(PartPreviewSourceType.DRAWING, UUID.randomUUID(), DrawingDimension.TWO_D);
        preview.registerSourceFile(UUID.randomUUID(), "source-a.step", "application/step", 10L);
        preview.completeProcessing(null, List.of(
                new DrawingArtifactPublication(
                        DrawingArtifactType.DERIVED_PDF,
                        UUID.randomUUID(),
                        "pdf",
                        "source-a.pdf",
                        "application/pdf",
                        20L,
                        true
                ),
                new DrawingArtifactPublication(
                        DrawingArtifactType.DERIVED_WEBP,
                        UUID.randomUUID(),
                        "webp",
                        "source-a.webp",
                        "image/webp",
                        30L,
                        true
                )
        ));

        preview.removeDerivedArtifacts();
        preview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, UUID.randomUUID(), DrawingDimension.THREE_D);
        preview.registerSourceFile(UUID.randomUUID(), "source-b.step", "application/step", 40L);

        assertEquals(1, preview.getArtifacts().size());
        assertEquals("source-b.step", preview.getOriginalFileKey());
        assertNull(preview.getPdfKey());
        assertNull(preview.getWebpKey());
        assertEquals(PartPreviewSourceType.PREVIEW_FILE, preview.getSourceType());
        assertEquals(DrawingDimension.THREE_D, preview.getDimension());
    }

    @Test
    void clearSource_모든_산출물을_함께_비운다() {
        PartPreview preview = PartPreview.create(UUID.randomUUID());
        preview.replaceSource(PartPreviewSourceType.DRAWING, UUID.randomUUID(), DrawingDimension.TWO_D);
        preview.registerSourceFile(UUID.randomUUID(), "source-a.step", "application/step", 10L);

        preview.clearSource();

        assertNull(preview.getSourceType());
        assertNull(preview.getSourceId());
        assertNull(preview.getDimension());
        assertEquals(0, preview.getArtifacts().size());
    }
}
