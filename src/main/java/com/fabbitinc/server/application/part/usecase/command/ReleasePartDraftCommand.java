package com.fabbitinc.server.application.part.usecase.command;

import java.util.UUID;

public record ReleasePartDraftCommand(
        UUID partId,
        UUID revisionId,
        String reason
) {
}
