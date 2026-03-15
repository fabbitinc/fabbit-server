package com.fabbitinc.server.presentation.activation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "활성화 그래프 헬스체크 응답")
public record HealthCheckResponse(
        @Schema(description = "전체 노드 수", example = "120")
        int totalNodes,
        @Schema(description = "전체 관계 수", example = "240")
        int totalRelationships,
        @Schema(description = "노드 라벨별 개수")
        Map<String, Integer> nodeCounts,
        @Schema(description = "관계 타입별 개수")
        Map<String, Integer> relationshipCounts,
        @Schema(description = "발견된 이슈 목록")
        List<HealthCheckIssueResponse> issues
) {
}
