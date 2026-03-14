package com.fabbitinc.server.application.part.usecase.result;

public record ApprovePartRevisionResult(
        String partNumber,
        String revisionCode
) {
}
