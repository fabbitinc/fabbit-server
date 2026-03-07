package com.fabbitinc.server.application.synthesisv2.usecase.command;

import java.util.List;
import java.util.UUID;

public record StartSynthesisV2Command(
        UUID mappingId,
        UUID projectId,
        boolean overwrite,
        List<StartSynthesisV2UploadCommand> uploads
) {
}
