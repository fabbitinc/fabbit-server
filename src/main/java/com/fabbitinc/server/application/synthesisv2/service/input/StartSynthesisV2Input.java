package com.fabbitinc.server.application.synthesisv2.service.input;

import java.util.List;
import java.util.UUID;

public record StartSynthesisV2Input(
        UUID mappingId,
        UUID projectId,
        boolean overwrite,
        List<SynthesisV2UploadInput> uploads
) {
}
