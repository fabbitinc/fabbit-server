package com.fabbitinc.server.application.activation.dto.response;

public record HealthCheckIssueResponse(
        String category,
        String severity,
        String message,
        int count
) {
}
