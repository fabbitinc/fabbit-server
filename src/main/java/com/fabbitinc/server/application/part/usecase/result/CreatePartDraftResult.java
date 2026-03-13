package com.fabbitinc.server.application.part.usecase.result;

import java.util.UUID;

public record CreatePartDraftResult(
        String partNumber,
        UUID draftId
) {
}
