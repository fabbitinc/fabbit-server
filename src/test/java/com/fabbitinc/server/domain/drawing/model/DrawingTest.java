package com.fabbitinc.server.domain.drawing.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DrawingTest {

    @Test
    void drawing_이름이_비어있으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Drawing.create(null, " "));

        assertEquals(Drawing.CODE_DRAWING_NAME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void drawing_변환상태_전이를_지원한다() {
        Drawing drawing = Drawing.create("D-001", "Assembly");

        assertEquals(DrawingStatus.DRAFT, drawing.getStatus());
        drawing.markConversionPending();
        assertEquals(DrawingConversionStatus.PENDING, drawing.getConversionStatus());

        drawing.markConversionCompleted("  files/a.pdf  ", "  files/a.webp  ");
        assertEquals(DrawingConversionStatus.COMPLETED, drawing.getConversionStatus());
        assertEquals("files/a.pdf", drawing.getPdfKey());
        assertEquals("files/a.webp", drawing.getThumbnailKey());

        drawing.markConversionFailed();
        assertEquals(DrawingConversionStatus.FAILED, drawing.getConversionStatus());
    }

    @Test
    void drawing_키필드는_blank면_null로_정규화한다() {
        Drawing drawing = Drawing.create("D-001", "Assembly");

        drawing.changeOriginalFileKey(" ");
        drawing.changePdfKey(" ");
        drawing.changeThumbnailKey(" ");

        assertNull(drawing.getOriginalFileKey());
        assertNull(drawing.getPdfKey());
        assertNull(drawing.getThumbnailKey());
    }

    @Test
    void drawing_markConversionCompleted_pdfKey가_blank면_예외를_던진다() {
        Drawing drawing = Drawing.create("D-001", "Assembly");

        DomainException ex = assertThrows(
                DomainException.class,
                () -> drawing.markConversionCompleted("   ", "files/a.webp")
        );

        assertEquals(Drawing.CODE_DRAWING_PDF_KEY_REQUIRED, ex.getDomainCode());
        assertNull(drawing.getConversionStatus());
        assertNull(drawing.getPdfKey());
        assertNull(drawing.getThumbnailKey());
    }

    @Test
    void drawing_markConversionCompleted_thumbnailKey가_blank면_예외를_던진다() {
        Drawing drawing = Drawing.create("D-001", "Assembly");

        DomainException ex = assertThrows(
                DomainException.class,
                () -> drawing.markConversionCompleted("files/a.pdf", "   ")
        );

        assertEquals(Drawing.CODE_DRAWING_THUMBNAIL_KEY_REQUIRED, ex.getDomainCode());
        assertNull(drawing.getConversionStatus());
        assertNull(drawing.getPdfKey());
        assertNull(drawing.getThumbnailKey());
    }
}
