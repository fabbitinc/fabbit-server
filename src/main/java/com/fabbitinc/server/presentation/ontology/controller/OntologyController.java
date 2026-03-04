package com.fabbitinc.server.presentation.ontology.controller;

import com.fabbitinc.server.application.ontology.dto.response.NodeSearchResponse;
import com.fabbitinc.server.application.ontology.dto.response.OntologySchemaResponse;
import com.fabbitinc.server.application.ontology.query.OntologyQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ontology")
@Tag(name = "ontology", description = "온톨로지 스키마/노드 검색 API")
public class OntologyController {

    private final OntologyQuery ontologyQuery;

    @Operation(
            summary = "GET /api/v1/ontology/schema",
            description = "온톨로지 스키마(노드/관계 정의)를 조회합니다"
    )
    @GetMapping("/schema")
    public OntologySchemaResponse getOntologySchema() {
        return ontologyQuery.getOntologySchema();
    }

    @Operation(
            summary = "GET /api/v1/ontology/nodes/search",
            description = "라벨(Part, Drawing, Supplier, Project)별 merge key 자동완성 목록을 조회합니다"
    )
    @GetMapping("/nodes/search")
    public NodeSearchResponse searchNodes(
            @RequestParam("label") String label,
            @RequestParam("search")
            @Size(min = 1, message = "search는 1자 이상이어야 합니다")
            String search,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit
    ) {
        return ontologyQuery.searchNodes(label, search, limit);
    }
}
