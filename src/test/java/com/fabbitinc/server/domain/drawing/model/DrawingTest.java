package com.fabbitinc.server.domain.drawing.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawingTest {

    @Test
    void drawing_이름이_비어있으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Drawing.create(null, " "));

        assertEquals(Drawing.CODE_DRAWING_NAME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void drawing_원본_파일_정보를_등록한다() {
        Drawing drawing = Drawing.create("D-001", "Assembly");
        UUID sourceFileId = UUID.randomUUID();

        assertEquals(DrawingStatus.DRAFT, drawing.getStatus());
        drawing.assignSourceFile(sourceFileId, DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D);
        drawing.changeOriginalFileKey("  files/a.pdf  ");

        assertEquals(sourceFileId, drawing.getSourceFileId());
        assertEquals("files/a.pdf", drawing.getOriginalFileKey());
        assertEquals(DrawingDimension.TWO_D, drawing.getDimension());
    }

    @Test
    void drawing_키필드는_blank면_null로_정규화한다() {
        Drawing drawing = Drawing.create("D-001", "Assembly");

        drawing.changeOriginalFileKey(" ");

        assertNull(drawing.getOriginalFileKey());
    }
}
