package com.fabbitinc.server.application.drawing.service;

import java.util.List;

public record DrawingPipelineResult(
        List<DrawingPipelineArtifact> artifacts
) {
}
