package com.fabbitinc.server.application.drawing.service;

import java.util.UUID;

public record DrawingJobClaim(
        UUID jobId,
        UUID drawingId,
        String pipelineKey,
        String profileKey
) {
}
