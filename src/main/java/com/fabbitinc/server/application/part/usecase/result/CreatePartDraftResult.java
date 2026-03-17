package com.fabbitinc.server.application.part.usecase.result;

import java.util.UUID;

public record CreatePartDraftResult(
        UUID partId,
        UUID revisionId
) {
}
