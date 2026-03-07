package com.fabbitinc.server.application.synthesisv2.usecase.command;

import java.util.Map;
import java.util.UUID;

public record StartSynthesisV2UploadCommand(
        UUID fileId,
        Map<String, String> rootContext
) {
}
