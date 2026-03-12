package com.fabbitinc.server.domain.drawing.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum DrawingExtension {
    // 도면
    DWG("dwg", DrawingSourceType.CAD_2D, DrawingDimension.TWO_D, false, DrawingRenderSourceGroup.PDF_PIPELINE),
    DXF("dxf", DrawingSourceType.CAD_2D, DrawingDimension.TWO_D, true, null),
    PDF("pdf", DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D, true, null),
    // 이미지
    PNG("png", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true, null),
    JPG("jpg", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true, null),
    JPEG("jpeg", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true, null),
    BMP("bmp", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true, null),
    TIF("tif", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true, null),
    TIFF("tiff", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true, null),
    WEBP("webp", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true, null),
    // 3D
    SLDPRT("sldprt", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, false, DrawingRenderSourceGroup.GLB_PIPELINE),
    SLDASM("sldasm", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, false, DrawingRenderSourceGroup.GLB_PIPELINE),
    STEP("step", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    STP("stp", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    IGES("iges", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    IGS("igs", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    BREP("brep", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    BRP("brp", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    STL("stl", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    OBJ("obj", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    THREE_MF("3mf", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    FBX("fbx", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    GLB("glb", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null),
    GLTF("gltf", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true, null);

    private final String format;
    private final DrawingSourceType sourceType;
    private final DrawingDimension dimension;
    private final boolean canStartPipelineDirectly;
    private final DrawingRenderSourceGroup requiredRenderSourceGroup;

    DrawingExtension(
            String format,
            DrawingSourceType sourceType,
            DrawingDimension dimension,
            boolean canStartPipelineDirectly,
            DrawingRenderSourceGroup requiredRenderSourceGroup
    ) {
        this.format = format;
        this.sourceType = sourceType;
        this.dimension = dimension;
        this.canStartPipelineDirectly = canStartPipelineDirectly;
        this.requiredRenderSourceGroup = requiredRenderSourceGroup;
    }

    public boolean requiresRenderSource() {
        return requiredRenderSourceGroup != null;
    }

    public static Optional<DrawingExtension> fromFileName(String fileName) {
        return fromFormat(extractFormat(fileName));
    }

    public static Optional<DrawingExtension> fromFormat(String format) {
        String normalized = normalizeFormat(format);
        if (normalized == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(extension -> extension.format.equals(normalized))
                .findFirst();
    }

    private static String extractFormat(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx >= fileName.length() - 1) {
            return null;
        }
        return fileName.substring(idx + 1);
    }

    private static String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return null;
        }
        String normalized = format.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }
}
