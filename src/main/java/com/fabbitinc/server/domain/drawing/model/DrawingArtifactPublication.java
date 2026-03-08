package com.fabbitinc.server.domain.drawing.model;

import java.util.UUID;

public record DrawingArtifactPublication(
        DrawingArtifactType artifactType,
        UUID fileId,
        String format,
        String storageKey,
        String contentType,
        long fileSize,
        boolean generated
) {
}
