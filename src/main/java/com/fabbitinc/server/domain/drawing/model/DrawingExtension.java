package com.fabbitinc.server.domain.drawing.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum DrawingExtension {
    // 도면
    DWG("dwg", DrawingSourceType.CAD_2D, DrawingDimension.TWO_D, false),
    DXF("dxf", DrawingSourceType.CAD_2D, DrawingDimension.TWO_D, true),
    PDF("pdf", DrawingSourceType.PDF_DOCUMENT, DrawingDimension.TWO_D, true),
    // 이미지
    PNG("png", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true),
    JPG("jpg", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true),
    JPEG("jpeg", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true),
    BMP("bmp", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true),
    TIF("tif", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true),
    TIFF("tiff", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true),
    WEBP("webp", DrawingSourceType.RASTER_IMAGE, DrawingDimension.TWO_D, true),
    // 3D
    SLDPRT("sldprt", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, false),
    SLDASM("sldasm", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, false),
    STEP("step", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    STP("stp", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    IGES("iges", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    IGS("igs", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    BREP("brep", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    BRP("brp", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    STL("stl", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    OBJ("obj", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    THREE_MF("3mf", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    FBX("fbx", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    GLB("glb", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true),
    GLTF("gltf", DrawingSourceType.CAD_3D, DrawingDimension.THREE_D, true);

    private final String format;
    private final DrawingSourceType sourceType;
    private final DrawingDimension dimension;
    private final boolean canStartPipelineDirectly;

    DrawingExtension(
            String format,
            DrawingSourceType sourceType,
            DrawingDimension dimension,
            boolean canStartPipelineDirectly
    ) {
        this.format = format;
        this.sourceType = sourceType;
        this.dimension = dimension;
        this.canStartPipelineDirectly = canStartPipelineDirectly;
    }

    public boolean requiresRenderSource() {
        return getRequiredRenderSourceGroup() != null;
    }

    public DrawingRenderSourceGroup getRequiredRenderSourceGroup() {
        return switch (this) {
            case DWG -> DrawingRenderSourceGroup.PDF_PIPELINE;
            case SLDPRT, SLDASM -> DrawingRenderSourceGroup.GLB_PIPELINE;
            default -> null;
        };
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
