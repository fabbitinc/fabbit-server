package com.fabbitinc.server.application.drawing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingExtension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;
import org.junit.jupiter.api.Test;

class DrawingSourceClassifierTest {

    private final DrawingSourceClassifier classifier = new DrawingSourceClassifier();

    @Test
    void classify_DWG는_업로드_가능한_2D_도면으로_분류한다() {
        DrawingSourceDescriptor descriptor = classifier.classify("sample.dwg");

        assertEquals(DrawingExtension.DWG, descriptor.extension());
        assertEquals(DrawingSourceType.CAD_2D, descriptor.sourceType());
        assertEquals(DrawingDimension.TWO_D, descriptor.dimension());
    }

    @Test
    void classify_SLDPRT는_업로드_가능한_3D_도면으로_분류한다() {
        DrawingSourceDescriptor descriptor = classifier.classify("sample.sldprt");

        assertEquals(DrawingExtension.SLDPRT, descriptor.extension());
        assertEquals(DrawingSourceType.CAD_3D, descriptor.sourceType());
        assertEquals(DrawingDimension.THREE_D, descriptor.dimension());
    }
}
