package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record DetachPartFileCommand(
        UUID partId,
        UUID fileId
) {
}
