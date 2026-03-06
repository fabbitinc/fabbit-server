package com.fabbitinc.server.application.activation.usecase.result;

import java.util.List;
import java.util.Map;

public record HealthCheckResult(
        int totalNodes,
        int totalRelationships,
        Map<String, Integer> nodeCounts,
        Map<String, Integer> relationshipCounts,
        List<HealthCheckIssueResult> issues
) {
}
