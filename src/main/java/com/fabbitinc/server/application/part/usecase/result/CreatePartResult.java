package com.fabbitinc.server.application.part.usecase.result;

import java.util.UUID;

public record CreatePartResult(
        UUID partId,
        UUID revisionId
) {
}
