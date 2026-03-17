package com.fabbitinc.server.application.part.usecase.result;

import java.util.UUID;

public record ReleasePartDraftResult(
        UUID partId,
        UUID revisionId
) {
}
