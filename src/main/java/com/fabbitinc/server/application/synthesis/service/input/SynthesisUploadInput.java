package com.fabbitinc.server.application.synthesis.service.input;

import java.util.Map;
import java.util.UUID;

public record SynthesisUploadInput(
        UUID fileId,
        Map<String, String> rootContext
) {
}
