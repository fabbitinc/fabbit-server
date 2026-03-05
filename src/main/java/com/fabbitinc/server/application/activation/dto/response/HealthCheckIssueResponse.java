package com.fabbitinc.server.application.activation.dto.response;

public record HealthCheckIssueResponse(
        HealthIssueCategory category,
        HealthIssueSeverity severity,
        String message,
        int count
) {
}
