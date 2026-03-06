package com.fabbitinc.server.application.activation.service.output;

import java.util.List;
import java.util.Map;

public record HealthCheckOutput(
        int totalNodes,
        int totalRelationships,
        Map<String, Integer> nodeCounts,
        Map<String, Integer> relationshipCounts,
        List<HealthCheckIssueOutput> issues
) {
}
