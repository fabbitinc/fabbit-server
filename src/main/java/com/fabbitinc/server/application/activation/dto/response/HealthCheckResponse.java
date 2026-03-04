package com.fabbitinc.server.application.activation.dto.response;

import java.util.List;
import java.util.Map;

public record HealthCheckResponse(
        int totalNodes,
        int totalRelationships,
        Map<String, Integer> nodeCounts,
        Map<String, Integer> relationshipCounts,
        List<HealthCheckIssueResponse> issues
) {
}
