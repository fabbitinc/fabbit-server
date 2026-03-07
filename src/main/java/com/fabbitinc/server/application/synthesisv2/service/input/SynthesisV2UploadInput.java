package com.fabbitinc.server.application.synthesisv2.service.input;

import java.util.Map;
import java.util.UUID;

public record SynthesisV2UploadInput(
        UUID fileId,
        Map<String, String> rootContext
) {
}
