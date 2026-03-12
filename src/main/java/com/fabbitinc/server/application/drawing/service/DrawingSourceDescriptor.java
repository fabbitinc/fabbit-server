package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.drawing.model.DrawingExtension;
import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;

public record DrawingSourceDescriptor(
        DrawingExtension extension,
        DrawingSourceType sourceType,
        DrawingDimension dimension
) {
}
