package com.fabbitinc.server.application.part.service;

import java.util.UUID;

public record PartPreviewJobClaim(
        UUID jobId,
        UUID partPreviewId,
        String pipelineKey,
        String profileKey
) {
}
