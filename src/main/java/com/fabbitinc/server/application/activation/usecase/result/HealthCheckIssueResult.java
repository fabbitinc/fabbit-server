package com.fabbitinc.server.application.activation.usecase.result;

public record HealthCheckIssueResult(
        String category,
        String severity,
        String message,
        int count
) {
}
