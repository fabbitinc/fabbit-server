package com.fabbitinc.server.presentation.ontology.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "온톨로지 노드 자동완성 응답")
public record NodeSearchResponse(
        @Schema(description = "검색 대상 노드 라벨", example = "Part")
        String nodeLabel,
        @Schema(description = "검색 결과 목록")
        List<NodeSearchItemResponse> items
) {
    @Schema(description = "온톨로지 노드 검색 항목")
    public record NodeSearchItemResponse(
            @Schema(description = "노드 값", example = "PART-001")
            String value,
            @Schema(description = "표시 라벨", example = "PART-001 / 메인 보드")
            String label
    ) {
    }
}
