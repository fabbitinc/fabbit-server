package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.drawing.model.DrawingArtifactType;

public record DrawingPipelineArtifact(
        DrawingArtifactType artifactType,
        String originalName,
        String contentType,
        byte[] bytes,
        boolean reuseSource
) {

    public static DrawingPipelineArtifact reuseSource(DrawingArtifactType artifactType) {
        return new DrawingPipelineArtifact(artifactType, null, null, null, true);
    }

    public static DrawingPipelineArtifact generated(
            DrawingArtifactType artifactType,
            String originalName,
            String contentType,
            byte[] bytes
    ) {
        return new DrawingPipelineArtifact(artifactType, originalName, contentType, bytes, false);
    }
}
