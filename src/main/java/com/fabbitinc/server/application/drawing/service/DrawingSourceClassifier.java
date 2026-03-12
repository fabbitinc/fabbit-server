package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.drawing.model.DrawingExtension;

public class DrawingSourceClassifier {

    public DrawingSourceDescriptor classify(String fileName) {
        DrawingExtension extension = DrawingExtension.fromFileName(fileName)
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 도면 파일 형식입니다: " + fileName));
        return new DrawingSourceDescriptor(
                extension,
                extension.getSourceType(),
                extension.getDimension()
        );
    }
}
