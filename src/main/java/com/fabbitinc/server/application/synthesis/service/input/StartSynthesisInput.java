package com.fabbitinc.server.application.synthesis.service.input;

import java.util.List;
import java.util.UUID;

public record StartSynthesisInput(
        UUID mappingId,
        UUID projectId,
        boolean overwrite,
        List<SynthesisUploadInput> uploads
) {
}
