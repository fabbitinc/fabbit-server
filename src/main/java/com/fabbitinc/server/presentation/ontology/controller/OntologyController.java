package com.fabbitinc.server.presentation.ontology.controller;

import com.fabbitinc.server.application.ontology.query.OntologyQuery;
import com.fabbitinc.server.application.ontology.query.condition.NodeSearchCondition;
import com.fabbitinc.server.application.ontology.query.result.NodeSearchResult;
import com.fabbitinc.server.application.ontology.query.result.OntologySchemaResult;
import com.fabbitinc.server.presentation.ontology.dto.response.NodeSearchResponse;
import com.fabbitinc.server.presentation.ontology.dto.response.OntologySchemaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/schema")
    public OntologySchemaResponse getOntologySchema() {
        return toOntologySchemaResponse(ontologyQuery.getSchema());
    }

    @Operation(
            summary = "GET /api/v1/ontology/nodes/search",
            description = "라벨(Part, Drawing, Supplier, Project)별 merge key 자동완성 목록을 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/nodes/search")
    public NodeSearchResponse searchNodes(
            @Parameter(description = "검색 대상 노드 라벨", example = "Part")
            @RequestParam("label") String label,
            @Parameter(description = "자동완성 검색어", example = "PCB")
            @RequestParam("search")
            @Size(min = 1, message = "search는 1자 이상이어야 합니다") String search,
            @Parameter(description = "조회 건수", example = "10")
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        return toNodeSearchResponse(ontologyQuery.search(new NodeSearchCondition(label, search, limit)));
    }

    private OntologySchemaResponse toOntologySchemaResponse(OntologySchemaResult result) {
        return new OntologySchemaResponse(
                result.name(),
                result.description(),
                result.nodeLabels().stream()
                        .map(node -> new OntologySchemaResponse.NodeLabelSchemaResponse(
                                node.label(),
                                node.description(),
                                node.properties().stream()
                                        .map(this::toPropertySchemaResponse)
                                        .toList(),
                                node.mergeKeys()
                        ))
                        .toList(),
                result.relationshipTypes().stream()
                        .map(relationship -> new OntologySchemaResponse.RelationshipTypeSchemaResponse(
                                relationship.relType(),
                                relationship.description(),
                                relationship.fromLabel(),
                                relationship.toLabel(),
                                relationship.properties().stream()
                                        .map(this::toPropertySchemaResponse)
                                        .toList()
                        ))
                        .toList()
        );
    }

    private OntologySchemaResponse.PropertySchemaResponse toPropertySchemaResponse(
            OntologySchemaResult.PropertyResult result
    ) {
        return new OntologySchemaResponse.PropertySchemaResponse(
                result.name(),
                result.description(),
                result.dataType(),
                result.required(),
                result.isMergeKey()
        );
    }

    private NodeSearchResponse toNodeSearchResponse(NodeSearchResult result) {
        return new NodeSearchResponse(
                result.nodeLabel(),
                result.items().stream()
                        .map(item -> new NodeSearchResponse.NodeSearchItemResponse(
                                item.value(),
                                item.label()
                        ))
                        .toList()
        );
    }
}
