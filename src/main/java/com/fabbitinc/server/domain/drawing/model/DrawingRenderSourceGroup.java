package com.fabbitinc.server.domain.drawing.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

@Getter
public enum DrawingRenderSourceGroup {
    PDF_PIPELINE(DrawingExtension.PDF, DrawingExtension.DXF),
    GLB_PIPELINE(
            DrawingExtension.STEP,
            DrawingExtension.STP,
            DrawingExtension.IGES,
            DrawingExtension.IGS,
            DrawingExtension.BREP,
            DrawingExtension.BRP,
            DrawingExtension.GLB
    );

    private final Set<DrawingExtension> allowedExtensions;

    DrawingRenderSourceGroup(DrawingExtension... allowedExtensions) {
        this.allowedExtensions = Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(allowedExtensions)));
    }

    public boolean supports(DrawingExtension extension) {
        return extension != null && allowedExtensions.contains(extension);
    }

    public List<String> getAllowedFormats() {
        return allowedExtensions.stream()
                .map(DrawingExtension::getFormat)
                .toList();
    }
}
