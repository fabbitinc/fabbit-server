package com.fabbitinc.server.application.synthesis.usecase.command;

import java.util.Map;
import java.util.UUID;

public record StartSynthesisUploadCommand(
        UUID fileId,
        Map<String, String> rootContext
) {
}
