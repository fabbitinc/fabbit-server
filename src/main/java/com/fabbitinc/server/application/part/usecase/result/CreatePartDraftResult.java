package com.fabbitinc.server.application.part.usecase.result;

public record CreatePartDraftResult(
        String partNumber,
        String draftKey
) {
}
