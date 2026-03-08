package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;
import java.util.Locale;
import java.util.Set;

public class DrawingSourceClassifier {

    private static final Set<String> CAD_2D_EXTENSIONS = Set.of(".dwg", ".dxf");
    private static final Set<String> PDF_EXTENSIONS = Set.of(".pdf");
    private static final Set<String> RASTER_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".bmp", ".tif", ".tiff");
    private static final Set<String> CAD_3D_EXTENSIONS = Set.of(".step", ".stp", ".iges", ".igs", ".stl", ".obj", ".glb", ".gltf");

    public DrawingSourceDescriptor classify(String fileName) {
        String extension = extractExtension(fileName);
        if (CAD_2D_EXTENSIONS.contains(extension)) {
            return new DrawingSourceDescriptor(DrawingSourceType.CAD_2D, DrawingDimension.TWO_D);
        }
        if (PDF_EXTENSIONS.contains(extension)) {
            return new DrawingSourceDescriptor(DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D);
        }
        if (RASTER_EXTENSIONS.contains(extension)) {
            return new DrawingSourceDescriptor(DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D);
        }
        if (CAD_3D_EXTENSIONS.contains(extension)) {
            return new DrawingSourceDescriptor(DrawingSourceType.CAD_3D, DrawingDimension.THREE_D);
        }
        throw new IllegalArgumentException("지원하지 않는 도면 파일 형식입니다: " + extension);
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx >= fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx).toLowerCase(Locale.ROOT);
    }
}
