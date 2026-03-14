package com.fabbitinc.server.application.part.usecase.result;

public record CreatePartResult(
        String partNumber,
        String draftKey
) {
}
