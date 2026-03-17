package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactPublication;
import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartPreviewTest {

    @Test
    void replaceSource_기존_파생_산출물을_제거하고_새_source로_교체한다() {
        PartPreview preview = PartPreview.create(UUID.randomUUID());
        preview.replaceSource(PartPreviewSourceType.DRAWING, UUID.randomUUID(), DrawingDimension.TWO_D);
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

        preview.clearArtifacts();
        preview.replaceSource(PartPreviewSourceType.PREVIEW_FILE, UUID.randomUUID(), DrawingDimension.THREE_D);

        assertEquals(0, preview.getArtifacts().size());
        assertNull(preview.getPdfKey());
        assertNull(preview.getWebpKey());
        assertEquals(PartPreviewSourceType.PREVIEW_FILE, preview.getSourceType());
        assertEquals(DrawingDimension.THREE_D, preview.getDimension());
    }

    @Test
    void clearSource_모든_산출물을_함께_비운다() {
        PartPreview preview = PartPreview.create(UUID.randomUUID());
        preview.replaceSource(PartPreviewSourceType.DRAWING, UUID.randomUUID(), DrawingDimension.TWO_D);
        preview.completeProcessing(null, List.of(
                new DrawingArtifactPublication(
                        DrawingArtifactType.DERIVED_WEBP,
                        UUID.randomUUID(),
                        "webp",
                        "source-a.webp",
                        "image/webp",
                        10L,
                        true
                )
        ));

        preview.clearSource();

        assertNull(preview.getSourceType());
        assertNull(preview.getSourceId());
        assertNull(preview.getDimension());
        assertEquals(0, preview.getArtifacts().size());
    }

    @Test
    void begin_mark_completeProcessing_상태가_순차적으로_전이된다() {
        PartPreview preview = PartPreview.create(UUID.randomUUID());
        UUID sourceId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        preview.replaceSource(PartPreviewSourceType.DRAWING, sourceId, DrawingDimension.TWO_D);
        preview.beginProcessing(jobId);
        preview.markProcessing(jobId);
        preview.completeProcessing(jobId, List.of(
                new DrawingArtifactPublication(
                        DrawingArtifactType.DERIVED_PDF,
                        UUID.randomUUID(),
                        "pdf",
                        "source-a.pdf",
                        "application/pdf",
                        20L,
                        true
                )
        ));

        assertEquals(PartPreviewProcessingStatus.COMPLETED, preview.getProcessingStatus());
        assertNull(preview.getCurrentJobId());
        assertEquals("source-a.pdf", preview.getPdfKey());
    }

    @Test
    void failProcessing_다른_jobId면_예외를_던진다() {
        PartPreview preview = PartPreview.create(UUID.randomUUID());

        preview.replaceSource(PartPreviewSourceType.DRAWING, UUID.randomUUID(), DrawingDimension.TWO_D);
        preview.beginProcessing(UUID.randomUUID());

        assertThrows(DomainException.class, () -> preview.failProcessing(UUID.randomUUID()));
    }

    @Test
    void failProcessing_같은_jobId면_failed로_전이된다() {
        PartPreview preview = PartPreview.create(UUID.randomUUID());
        UUID jobId = UUID.randomUUID();

        preview.replaceSource(PartPreviewSourceType.DRAWING, UUID.randomUUID(), DrawingDimension.TWO_D);
        preview.beginProcessing(jobId);
        preview.failProcessing(jobId);

        assertEquals(PartPreviewProcessingStatus.FAILED, preview.getProcessingStatus());
        assertNull(preview.getCurrentJobId());
    }

    @Test
    void addPreviewFile_같은_원본파일이면_relation을_중복생성하지_않는다() {
        PartPreview preview = PartPreview.create(UUID.randomUUID());
        UUID fileId = UUID.randomUUID();

        PartPreviewFile first = preview.addPreviewFile(fileId);
        PartPreviewFile second = preview.addPreviewFile(fileId);

        assertEquals(first.getId(), second.getId());
        assertEquals(1, preview.getPreviewFiles().size());
    }
}
