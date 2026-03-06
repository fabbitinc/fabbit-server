package com.fabbitinc.server.application.file.usecase.command;

import java.util.UUID;

public record CompleteFileCommand(
        UUID fileId
) {
}
