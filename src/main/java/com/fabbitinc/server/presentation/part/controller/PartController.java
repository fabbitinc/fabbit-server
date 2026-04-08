package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toCategoryLookupResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toCategoryStatsResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartDetailResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartFilterOptionsResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartImpactAnalysisResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartInProgressListResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartListResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartLookupResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartRevisionLookupResponse;

import com.fabbitinc.server.application.part.query.PartImpactAnalysisQuery;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartExportCondition;
import com.fabbitinc.server.application.part.query.condition.PartImpactAnalysisCondition;
import com.fabbitinc.server.application.part.query.condition.PartInProgressListCondition;
import com.fabbitinc.server.application.part.query.condition.PartInProgressStatusFilter;
import com.fabbitinc.server.application.part.query.condition.PartListCondition;
import com.fabbitinc.server.application.part.query.condition.PartLookupCondition;
import com.fabbitinc.server.application.part.query.condition.PartRevisionLookupCondition;
import com.fabbitinc.server.application.part.usecase.ChangePartLifecycleStateUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartUseCase;
import com.fabbitinc.server.application.part.usecase.command.CreatePartCommand;
import com.fabbitinc.server.application.part.usecase.result.CreatePartResult;
import com.fabbitinc.server.presentation.part.request.ChangePartLifecycleStateRequest;
import com.fabbitinc.server.presentation.part.request.CreatePartRequest;
import com.fabbitinc.server.presentation.part.request.PartRevisionLookupStatusRequest;
import com.fabbitinc.server.presentation.part.response.CategoryLookupResponse;
import com.fabbitinc.server.presentation.part.response.CategoryStatsResponse;
import com.fabbitinc.server.presentation.part.response.ChangePartLifecycleStateResponse;
import com.fabbitinc.server.presentation.part.response.PartDetailResponse;
import com.fabbitinc.server.presentation.part.response.PartFilterOptionsResponse;
import com.fabbitinc.server.presentation.part.response.PartImpactAnalysisResponse;
import com.fabbitinc.server.presentation.part.response.PartInProgressListResponse;
import com.fabbitinc.server.presentation.part.response.PartListResponse;
import com.fabbitinc.server.presentation.part.response.PartLookupResponse;
import com.fabbitinc.server.presentation.part.response.PartRevisionLookupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "parts", description = "부품 관리/조회 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PartQuery partQuery;
    private final PartImpactAnalysisQuery partImpactAnalysisQuery;
    private final CreatePartUseCase createPartUseCase;
    private final ChangePartLifecycleStateUseCase changePartLifecycleStateUseCase;

    @Operation(operationId = "partCreate", summary = "부품을 생성하고 초안 상세를 반환합니다", description = "부품을 생성하고 생성된 초기 초안 상세를 반환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "409", description = "리소스 충돌"),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartDetailResponse createPart(@Valid @RequestBody CreatePartRequest request) {
        CreatePartResult result = createPartUseCase.execute(new CreatePartCommand(
                request.partNumber(),
                request.categoryId(),
                request.itemType(),
                request.name(),
                request.material(),
                request.unit(),
                request.description(),
                request.lifecycleState(),
                request.leadTimeDays(),
                request.extendedProperties(),
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(result.partId(), result.revisionId())));
    }

    @Operation(operationId = "partLookup", summary = "품번/품명으로 경량 Part 목록을 조회합니다", description = "품번/품명으로 경량 Part 목록을 조회합니다")
    @GetMapping("/lookup")
    public PartLookupResponse lookupParts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        return toPartLookupResponse(partQuery.lookup(new PartLookupCondition(search, limit)));
    }

    @Operation(operationId = "partLookupRevisions", summary = "리비전 lookup 목록을 조회합니다", description = "상태와 작성자 필터를 적용해 리비전 lookup 목록을 조회합니다. 기본값은 status=DRAFT, mine_only=true 입니다")
    @GetMapping("/revisions/lookup")
    public PartRevisionLookupResponse lookupRevisions(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", defaultValue = "DRAFT") PartRevisionLookupStatusRequest status,
            @RequestParam(value = "mine_only", defaultValue = "true") boolean mineOnly,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        return toPartRevisionLookupResponse(partQuery.lookupRevisions(
                new PartRevisionLookupCondition(search, limit, status.toDomainStatus(), mineOnly)
        ));
    }

    @Operation(operationId = "partExport", summary = "필터링된 Part 목록을 Excel 파일로 내보냅니다", description = "필터링된 Part 목록을 Excel(.xlsx) 파일로 내보냅니다")
    @GetMapping(value = "/export", produces = EXCEL_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportParts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "lifecycle_state", required = false) String lifecycleState,
            @RequestParam(value = "has_drawing", required = false) Boolean hasDrawing,
            @RequestParam(value = "has_children", required = false) Boolean hasChildren,
            @RequestParam(value = "mapping_id", required = false) UUID mappingId,
            @RequestParam(value = "part_ids", required = false) List<UUID> partIds,
            @RequestParam(value = "project_id", required = false) UUID projectId
    ) {
        byte[] content = partQuery.export(new PartExportCondition(
                search,
                category,
                lifecycleState,
                hasDrawing,
                hasChildren,
                partIds,
                mappingId,
                projectId
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE));
        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''%EB%B6%80%ED%92%88%EB%AA%A9%EB%A1%9D.xlsx"
        );
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @Operation(operationId = "partListCategories", summary = "카테고리별 부품 개수를 조회합니다", description = "카테고리별 부품 개수를 조회합니다")
    @GetMapping("/categories")
    public CategoryStatsResponse listCategories() {
        return toCategoryStatsResponse(partQuery.listCategories());
    }

    @Operation(operationId = "partLookupCategories", summary = "카테고리 문자열 목록을 경량 조회합니다", description = "카테고리 문자열 목록을 경량 조회합니다")
    @GetMapping("/categories/lookup")
    public CategoryLookupResponse lookupCategories() {
        return toCategoryLookupResponse(partQuery.lookupCategories());
    }

    @Operation(operationId = "partGetFilterOptions", summary = "Part 목록 필터 옵션을 조회합니다", description = "Part 목록 필터 옵션(카테고리/수명주기 상태)을 조회합니다")
    @GetMapping("/filter-options")
    public PartFilterOptionsResponse getFilterOptions() {
        return toPartFilterOptionsResponse(partQuery.getFilterOptions());
    }

    @Operation(operationId = "partList", summary = "Part 목록을 조회합니다", description = "검색/필터 조건과 함께 Part 목록을 조회합니다")
    @GetMapping
    public PartListResponse listParts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "lifecycle_state", required = false) String lifecycleState,
            @RequestParam(value = "has_drawing", required = false) Boolean hasDrawing,
            @RequestParam(value = "has_children", required = false) Boolean hasChildren,
            @RequestParam(value = "has_stale_child_reference", required = false) Boolean hasStaleChildReference,
            @RequestParam(value = "project_id", required = false) UUID projectId,
            @RequestParam(value = "next_cursor", required = false) String nextCursor,
            @RequestParam(value = "prev_cursor", required = false) String prevCursor,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit
    ) {
        return toPartListResponse(partQuery.list(new PartListCondition(
                search,
                category,
                lifecycleState,
                hasDrawing,
                hasChildren,
                hasStaleChildReference,
                projectId,
                nextCursor,
                prevCursor,
                limit
        )));
    }

    @Operation(operationId = "partListInProgress", summary = "진행중 부품 작업함 목록을 조회합니다", description = "검색/필터 조건과 함께 진행중 부품 작업함 목록을 조회합니다")
    @GetMapping("/in-progress")
    public PartInProgressListResponse listInProgressParts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "lifecycle_state", required = false) String lifecycleState,
            @RequestParam(value = "statuses", required = false) List<PartInProgressStatusFilter> statuses,
            @RequestParam(value = "mine_only", defaultValue = "false") boolean mineOnly,
            @RequestParam(value = "has_drawing", required = false) Boolean hasDrawing,
            @RequestParam(value = "has_children", required = false) Boolean hasChildren,
            @RequestParam(value = "project_id", required = false) UUID projectId,
            @RequestParam(value = "next_cursor", required = false) String nextCursor,
            @RequestParam(value = "prev_cursor", required = false) String prevCursor,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit
    ) {
        return toPartInProgressListResponse(partQuery.listInProgress(new PartInProgressListCondition(
                search,
                category,
                lifecycleState,
                statuses,
                mineOnly,
                hasDrawing,
                hasChildren,
                projectId,
                nextCursor,
                prevCursor,
                limit
        )));
    }

    @Operation(operationId = "partChangeLifecycleState", summary = "부품의 수명주기 상태를 변경합니다", description = "부품의 수명주기 상태를 변경합니다 (ACTIVE → EOL → OBSOLETE)")
    @PostMapping("/{partId}/lifecycle")
    public ChangePartLifecycleStateResponse changeLifecycleState(
            @PathVariable UUID partId,
            @Valid @RequestBody ChangePartLifecycleStateRequest request
    ) {
        ChangePartLifecycleStateUseCase.ChangePartLifecycleStateResult result =
                changePartLifecycleStateUseCase.execute(
                        new ChangePartLifecycleStateUseCase.ChangePartLifecycleStateCommand(partId, request.targetState())
                );
        return new ChangePartLifecycleStateResponse(result.partId(), result.lifecycleState());
    }

    @Operation(operationId = "partGetImpactAnalysis", summary = "부품 영향 분석", description = "특정 부품 변경 시 영향받는 상위 BOM, 프로젝트, 추천 리뷰어를 분석합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "영향 분석 성공"),
            @ApiResponse(responseCode = "404", description = "부품을 찾을 수 없음")
    })
    @GetMapping("/{partId}/impact-analysis")
    public PartImpactAnalysisResponse getImpactAnalysis(@PathVariable UUID partId) {
        return toPartImpactAnalysisResponse(
                partImpactAnalysisQuery.analyze(new PartImpactAnalysisCondition(partId))
        );
    }

}
