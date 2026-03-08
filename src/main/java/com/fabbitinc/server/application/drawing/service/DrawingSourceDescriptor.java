package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.drawing.model.DrawingDimension;
import com.fabbitinc.server.domain.drawing.model.DrawingSourceType;

public record DrawingSourceDescriptor(
        DrawingSourceType sourceType,
        DrawingDimension dimension
) {
}
