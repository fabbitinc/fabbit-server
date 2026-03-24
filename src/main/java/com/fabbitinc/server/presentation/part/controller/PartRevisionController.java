package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toBomTreeResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartBomResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartDetailResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartProjectsResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartRevisionDiffResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartRevisionHistoryResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartSuppliersResponse;

import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.BomTreeCondition;
import com.fabbitinc.server.application.part.query.condition.BomTreeExportCondition;
import com.fabbitinc.server.application.part.query.condition.PartBomCondition;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.part.query.condition.PartRevisionDiffCondition;
import com.fabbitinc.server.application.part.query.condition.PartRevisionHistoryCondition;
import com.fabbitinc.server.application.part.query.condition.PartSuppliersCondition;
import com.fabbitinc.server.presentation.part.response.BomTreeResponse;
import com.fabbitinc.server.presentation.part.response.PartBomResponse;
import com.fabbitinc.server.presentation.part.response.PartDetailResponse;
import com.fabbitinc.server.presentation.part.response.PartProjectsResponse;
import com.fabbitinc.server.presentation.part.response.PartRevisionDiffResponse;
import com.fabbitinc.server.presentation.part.response.PartRevisionHistoryResponse;
import com.fabbitinc.server.presentation.part.response.PartSuppliersResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "part-revisions", description = "부품 리비전 조회 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartRevisionController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PartQuery partQuery;

    @Operation(summary = "PartRevision 상세를 조회합니다", description = "부품 리비전 상세와 관계 카운트를 조회합니다")
    @GetMapping("/{partId}/revisions/{revisionId}")
    public PartDetailResponse get(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId
    ) {
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(partId, revisionId)));
    }

    @Operation(summary = "Part 변경 이력을 조회합니다", description = "공식 리비전 카드와 초안 시도 이력을 함께 조회합니다")
    @GetMapping("/{partId}/history")
    public PartRevisionHistoryResponse getHistory(@PathVariable UUID partId) {
        return toPartRevisionHistoryResponse(partQuery.getHistory(new PartRevisionHistoryCondition(partId)));
    }

    @Operation(summary = "기준 리비전 대비 상세 diff를 조회합니다", description = "이전 리비전 또는 지정한 기준 리비전 ID 대비 상세 diff를 조회합니다")
    @GetMapping("/{partId}/revisions/{revisionId}/diff")
    public PartRevisionDiffResponse getDiff(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @RequestParam(value = "base_revision_id", required = false) UUID baseRevisionId
    ) {
        return toPartRevisionDiffResponse(partQuery.getDiff(new PartRevisionDiffCondition(partId, revisionId, baseRevisionId)));
    }

    @Operation(summary = "직접 자식/부모 BOM을 조회합니다", description = "리비전 기준 직접 자식/직접 부모 BOM 관계를 조회합니다")
    @GetMapping("/{partId}/revisions/{revisionId}/bom")
    public PartBomResponse getBom(@PathVariable UUID partId, @PathVariable UUID revisionId) {
        return toPartBomResponse(partQuery.get(new PartBomCondition(partId, revisionId)));
    }

    @Operation(summary = "BOM 트리를 조회합니다", description = "정전개 또는 역전개 BOM 트리를 조회합니다")
    @GetMapping("/{partId}/revisions/{revisionId}/bom/tree")
    public BomTreeResponse getBomTree(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction
    ) {
        return toBomTreeResponse(partQuery.getBomTree(new BomTreeCondition(partId, revisionId, direction)));
    }

    @Operation(summary = "BOM 트리를 Excel로 내보냅니다", description = "BOM 트리를 Excel(.xlsx) 파일로 내보냅니다")
    @GetMapping(value = "/{partId}/revisions/{revisionId}/bom/tree/export", produces = EXCEL_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportBomTree(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction,
            @RequestParam(value = "mapping_id", required = false) UUID mappingId
    ) {
        byte[] content = partQuery.exportBomTree(new BomTreeExportCondition(partId, revisionId, direction, mappingId));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BOM.xlsx");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @Operation(summary = "연결된 프로젝트 목록을 조회합니다", description = "리비전이 속한 프로젝트 목록을 조회합니다")
    @GetMapping("/{partId}/revisions/{revisionId}/projects")
    public PartProjectsResponse getProjects(@PathVariable UUID partId, @PathVariable UUID revisionId) {
        return toPartProjectsResponse(partQuery.get(new PartProjectsCondition(partId, revisionId)));
    }

    @Operation(summary = "연결된 공급사 목록을 조회합니다", description = "리비전에 연결된 공급사 목록을 조회합니다")
    @GetMapping("/{partId}/revisions/{revisionId}/suppliers")
    public PartSuppliersResponse getSuppliers(@PathVariable UUID partId, @PathVariable UUID revisionId) {
        return toPartSuppliersResponse(partQuery.get(new PartSuppliersCondition(partId, revisionId)));
    }
}
