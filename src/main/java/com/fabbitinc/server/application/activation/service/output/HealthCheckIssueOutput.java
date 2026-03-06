package com.fabbitinc.server.application.activation.service.output;

public record HealthCheckIssueOutput(
        String category,
        String severity,
        String message,
        int count
) {
}
