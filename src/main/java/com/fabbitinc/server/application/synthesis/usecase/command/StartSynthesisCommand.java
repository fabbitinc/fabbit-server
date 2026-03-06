package com.fabbitinc.server.application.synthesis.usecase.command;

import java.util.List;
import java.util.UUID;

public record StartSynthesisCommand(
        UUID mappingId,
        UUID projectId,
        boolean overwrite,
        List<StartSynthesisUploadCommand> uploads
) {
}
