package com.fabbitinc.server.application.drawing.service;

import com.fabbitinc.server.domain.file.model.File;
import java.nio.file.Path;
import java.util.UUID;

public record DrawingPipelineCommand(
        UUID drawingId,
        File sourceFile,
        Path inputPath,
        Path workDir
) {
}
