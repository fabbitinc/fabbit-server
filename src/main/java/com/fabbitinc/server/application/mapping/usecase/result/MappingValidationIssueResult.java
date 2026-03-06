package com.fabbitinc.server.application.mapping.usecase.result;

public record MappingValidationIssueResult(
        String code,
        String severity,
        String message,
        String path,
        String dismissedReason
) {
}
