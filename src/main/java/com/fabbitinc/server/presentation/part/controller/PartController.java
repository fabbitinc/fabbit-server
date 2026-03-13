package com.fabbitinc.server.presentation.part.controller;

import com.fabbitinc.server.application.drawing.dto.request.RegisterDrawingRequest;
import com.fabbitinc.server.application.drawing.dto.response.RegisterDrawingResponse;
import com.fabbitinc.server.application.part.dto.request.AttachFilesRequest;
import com.fabbitinc.server.application.part.dto.request.ChangePartPreviewRequest;
import com.fabbitinc.server.application.part.dto.request.CreatePartRequest;
import com.fabbitinc.server.application.part.dto.request.RenameCategoryRequest;
import com.fabbitinc.server.application.part.dto.request.UpdatePartRevisionRequest;
import com.fabbitinc.server.application.part.dto.response.PartAttachmentItemResponse;
import com.fabbitinc.server.application.part.dto.response.BomChildResponse;
import com.fabbitinc.server.application.part.dto.response.BomParentResponse;
import com.fabbitinc.server.application.part.dto.response.BomTreeNodeResponse;
import com.fabbitinc.server.application.part.dto.response.BomTreeResponse;
import com.fabbitinc.server.application.part.dto.response.CategoryLookupResponse;
import com.fabbitinc.server.application.part.dto.response.CategoryStatsItemResponse;
import com.fabbitinc.server.application.part.dto.response.CategoryStatsResponse;
import com.fabbitinc.server.application.part.dto.response.PartBomResponse;
import com.fabbitinc.server.application.part.dto.response.PartDetailResponse;
import com.fabbitinc.server.application.part.dto.response.PartFilesResponse;
import com.fabbitinc.server.application.part.dto.response.PartFilterOptionsResponse;
import com.fabbitinc.server.application.part.dto.response.PartListResponse;
import com.fabbitinc.server.application.part.dto.response.PartLookupItemResponse;
import com.fabbitinc.server.application.part.dto.response.PartLookupResponse;
import com.fabbitinc.server.application.part.dto.response.PartOwnerUserSummaryResponse;
import com.fabbitinc.server.application.part.dto.response.PartProjectSummaryResponse;
import com.fabbitinc.server.application.part.dto.response.PartProjectsResponse;
import com.fabbitinc.server.application.part.dto.response.PartPreviewProcessingResponse;
import com.fabbitinc.server.application.part.dto.response.PartPreviewResponse;
import com.fabbitinc.server.application.part.dto.response.PartSummaryResponse;
import com.fabbitinc.server.application.part.dto.response.PartSuppliersResponse;
import com.fabbitinc.server.application.part.dto.response.RelatedSupplierResponse;
import com.fabbitinc.server.application.part.dto.response.RenameCategoryResponse;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.PartPreviewProcessingQuery;
import com.fabbitinc.server.application.part.query.condition.BomTreeCondition;
import com.fabbitinc.server.application.part.query.condition.BomTreeExportCondition;
import com.fabbitinc.server.application.part.query.condition.FileItemsCondition;
import com.fabbitinc.server.application.part.query.condition.PartBomCondition;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartDraftDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartExportCondition;
import com.fabbitinc.server.application.part.query.condition.PartFilesCondition;
import com.fabbitinc.server.application.part.query.condition.PartListCondition;
import com.fabbitinc.server.application.part.query.condition.PartLookupCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewProcessingCondition;
import com.fabbitinc.server.application.part.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.part.query.condition.PartSuppliersCondition;
import com.fabbitinc.server.application.part.query.result.BomTreeResult;
import com.fabbitinc.server.application.part.query.result.CategoryLookupResult;
import com.fabbitinc.server.application.part.query.result.CategoryStatsResult;
import com.fabbitinc.server.application.part.query.result.PartBomResult;
import com.fabbitinc.server.application.part.query.result.PartDetailResult;
import com.fabbitinc.server.application.part.query.result.PartFilesResult;
import com.fabbitinc.server.application.part.query.result.PartFilterOptionsResult;
import com.fabbitinc.server.application.part.query.result.PartListResult;
import com.fabbitinc.server.application.part.query.result.PartLookupResult;
import com.fabbitinc.server.application.part.query.result.PartProjectsResult;
import com.fabbitinc.server.application.part.query.result.PartSuppliersResult;
import com.fabbitinc.server.application.part.usecase.AttachPartFilesUseCase;
import com.fabbitinc.server.application.part.usecase.ChangePartPreviewUseCase;
import com.fabbitinc.server.application.part.usecase.ClearPartPreviewUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.DetachPartFileUseCase;
import com.fabbitinc.server.application.part.usecase.RegisterPartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.RenameCategoryUseCase;
import com.fabbitinc.server.application.part.usecase.UpdatePartRevisionUseCase;
import com.fabbitinc.server.application.part.usecase.command.AttachPartFilesCommand;
import com.fabbitinc.server.application.part.usecase.command.ChangePartPreviewCommand;
import com.fabbitinc.server.application.part.usecase.command.ClearPartPreviewCommand;
import com.fabbitinc.server.application.part.usecase.command.CreatePartCommand;
import com.fabbitinc.server.application.part.usecase.command.CreatePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.command.DetachPartFileCommand;
import com.fabbitinc.server.application.part.usecase.command.RegisterPartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.command.RenameCategoryCommand;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.result.AttachPartFilesResult;
import com.fabbitinc.server.application.part.usecase.result.CreatePartResult;
import com.fabbitinc.server.application.part.usecase.result.CreatePartDraftResult;
import com.fabbitinc.server.application.part.usecase.result.RegisterPartDrawingResult;
import com.fabbitinc.server.application.part.usecase.result.RenameCategoryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final CreatePartUseCase createPartUseCase;
    private final CreatePartDraftUseCase createPartDraftUseCase;
    private final UpdatePartRevisionUseCase updatePartRevisionUseCase;
    private final RenameCategoryUseCase renameCategoryUseCase;
    private final AttachPartFilesUseCase attachPartFilesUseCase;
    private final DetachPartFileUseCase detachPartFileUseCase;
    private final PartPreviewProcessingQuery partPreviewProcessingQuery;
    private final RegisterPartDrawingUseCase registerPartDrawingUseCase;
    private final DeletePartDrawingUseCase deletePartDrawingUseCase;
    private final ChangePartPreviewUseCase changePartPreviewUseCase;
    private final ClearPartPreviewUseCase clearPartPreviewUseCase;

    @Operation(
            summary = "POST /api/v1/parts",
            description = "부품을 생성하고 생성 직후 상세 정보를 반환합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "리소스 충돌"),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartDetailResponse createPart(
            @Parameter(description = "부품 생성 요청")
            @Valid @RequestBody CreatePartRequest request
    ) {
        CreatePartResult result = createPartUseCase.execute(new CreatePartCommand(
                request.partNumber(),
                request.name(),
                request.material(),
                request.unit(),
                request.description(),
                request.category(),
                request.isPhantom(),
                request.lifecycleState(),
                request.leadTimeDays(),
                request.extendedProperties()
        ));
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(
                result.partNumber(),
                result.draftId()
        )));
    }

    @Operation(
            summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/drafts",
            description = "기준 리비전에서 새 초안 리비전을 생성합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "리소스 충돌"),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public PartDetailResponse createDraft(
            @Parameter(description = "기준 품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode
    ) {
        CreatePartDraftResult result = createPartDraftUseCase.execute(new CreatePartDraftCommand(
                partNumber,
                revisionCode
        ));
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(
                result.partNumber(),
                result.draftId()
        )));
    }

    @Operation(
            summary = "PATCH /api/v1/parts/{partNumber}/drafts/{draftId}",
            description = "DRAFT 상태의 부품 초안을 수정합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "리소스 충돌"),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PatchMapping("/{partNumber}/drafts/{draftId}")
    public PartDetailResponse updateDraft(
            @Parameter(description = "수정할 품번")
            @PathVariable String partNumber,
            @Parameter(description = "수정할 초안 식별자")
            @PathVariable UUID draftId,
            @Parameter(description = "부품 초안 수정 요청")
            @Valid @RequestBody UpdatePartRevisionRequest request
    ) {
        updatePartRevisionUseCase.execute(new UpdatePartRevisionCommand(
                partNumber,
                draftId,
                request.getName(),
                request.isNameSet(),
                request.getMaterial(),
                request.isMaterialSet(),
                request.getUnit(),
                request.isUnitSet(),
                request.getDescription(),
                request.isDescriptionSet(),
                request.getCategory(),
                request.isCategorySet(),
                request.getPhantom(),
                request.isPhantomSet(),
                request.getLeadTimeDays(),
                request.isLeadTimeDaysSet(),
                request.getExtendedProperties(),
                request.isExtendedPropertiesSet()
        ));
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, draftId)));
    }

    @Operation(
            summary = "GET /api/v1/parts/lookup",
            description = "품번/품명으로 경량 Part 목록을 조회합니다"
    )
    @GetMapping("/lookup")
    public PartLookupResponse lookupParts(
            @Parameter(description = "품번/품명 검색어", example = "M3")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "조회 건수", example = "10")
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        return toPartLookupResponse(partQuery.lookup(new PartLookupCondition(search, limit)));
    }

    @Operation(
            summary = "GET /api/v1/parts/export",
            description = "필터링된 Part 목록을 Excel(.xlsx) 파일로 내보냅니다"
    )
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

    @Operation(
            summary = "GET /api/v1/parts/categories",
            description = "카테고리별 부품 개수를 조회합니다"
    )
    @GetMapping("/categories")
    public CategoryStatsResponse listCategories(
) {
        return toCategoryStatsResponse(partQuery.listCategories());
    }

    @Operation(
            summary = "GET /api/v1/parts/categories/lookup",
            description = "카테고리 문자열 목록을 경량 조회합니다"
    )
    @GetMapping("/categories/lookup")
    public CategoryLookupResponse lookupCategories(
) {
        return toCategoryLookupResponse(partQuery.lookupCategories());
    }

    @Operation(
            summary = "GET /api/v1/parts/filter-options",
            description = "Part 목록 필터 옵션(카테고리/수명주기 상태)을 조회합니다"
    )
    @GetMapping("/filter-options")
    public PartFilterOptionsResponse getFilterOptions(
) {
        return toPartFilterOptionsResponse(partQuery.getFilterOptions());
    }

    @Operation(
            summary = "GET /api/v1/parts",
            description = "Part 목록을 검색/필터 조건과 함께 조회합니다"
    )
    @GetMapping
    public PartListResponse listParts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "lifecycle_state", required = false) String lifecycleState,
            @RequestParam(value = "has_drawing", required = false) Boolean hasDrawing,
            @RequestParam(value = "has_children", required = false) Boolean hasChildren,
            @RequestParam(value = "project_id", required = false) UUID projectId,
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다") int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit
    ) {
        return toPartListResponse(partQuery.list(new PartListCondition(
                search,
                category,
                lifecycleState,
                hasDrawing,
                hasChildren,
                projectId,
                offset,
                limit
        )));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partNumber}/drafts/{draftId}",
            description = "Part 초안 상세 정보와 관계 카운트(children/parents/suppliers/files/projects)를 조회합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
    })
    @GetMapping("/{partNumber}/drafts/{draftId}")
    public PartDetailResponse getDraft(
            @Parameter(description = "조회할 품번")
            @PathVariable String partNumber,
            @Parameter(description = "조회할 초안 식별자")
            @PathVariable UUID draftId
    ) {
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, draftId)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}",
            description = "Part 상세 정보와 관계 카운트(children/parents/suppliers/files/projects)를 조회합니다"
    )
    @GetMapping("/{partNumber}/revisions/{revisionCode}")
    public PartDetailResponse getPart(
            @Parameter(description = "조회할 품번")
            @PathVariable String partNumber,
            @Parameter(description = "조회할 리비전 코드")
            @PathVariable String revisionCode
    ) {
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(partNumber, revisionCode)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/bom",
            description = "Part의 직접 자식/직접 부모 BOM 관계(1-depth)를 조회합니다"
    )
    @GetMapping("/{partNumber}/revisions/{revisionCode}/bom")
    public PartBomResponse getPartBom(
            @Parameter(description = "BOM을 조회할 품번")
            @PathVariable String partNumber,
            @Parameter(description = "BOM을 조회할 리비전 코드")
            @PathVariable String revisionCode
    ) {
        return toPartBomResponse(partQuery.get(new PartBomCondition(partNumber, revisionCode)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/bom/tree",
            description = "Part BOM 트리를 정전개(forward) 또는 역전개(reverse)로 조회합니다"
    )
    @GetMapping("/{partNumber}/revisions/{revisionCode}/bom/tree")
    public BomTreeResponse getBomTree(
            @Parameter(description = "BOM 트리를 조회할 품번")
            @PathVariable String partNumber,
            @Parameter(description = "BOM 트리를 조회할 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "정전개/역전개 방향", example = "FORWARD")
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction
    ) {
        return toBomTreeResponse(partQuery.getBomTree(new BomTreeCondition(partNumber, revisionCode, direction)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/bom/tree/export",
            description = "Part BOM 트리를 Excel(.xlsx) 파일로 내보냅니다"
    )
    @GetMapping(value = "/{partNumber}/revisions/{revisionCode}/bom/tree/export", produces = EXCEL_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportBomTree(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction,
            @RequestParam(value = "mapping_id", required = false) UUID mappingId
    ) {
        byte[] content = partQuery.exportBomTree(new BomTreeExportCondition(
                partNumber,
                revisionCode,
                direction,
                mappingId
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BOM.xlsx");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @Operation(
            summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/projects",
            description = "해당 Part가 소속된 프로젝트 목록을 조회합니다"
    )
    @GetMapping("/{partNumber}/revisions/{revisionCode}/projects")
    public PartProjectsResponse getPartProjects(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartProjectsResponse(partQuery.get(new PartProjectsCondition(partNumber, revisionCode)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/files",
            description = "Part에 연결된 업로드 완료 파일 목록을 조회합니다"
    )
    @GetMapping("/{partNumber}/revisions/{revisionCode}/files")
    public PartFilesResponse getPartFiles(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartFilesResponse(partQuery.get(new PartFilesCondition(partNumber, revisionCode)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/suppliers",
            description = "Part에 연결된 공급사 목록을 조회합니다"
    )
    @GetMapping("/{partNumber}/revisions/{revisionCode}/suppliers")
    public PartSuppliersResponse getPartSuppliers(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartSuppliersResponse(partQuery.get(new PartSuppliersCondition(partNumber, revisionCode)));
    }

    @Operation(
            summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/files",
            description = "업로드 완료 파일들을 Part에 배치 연결합니다"
    )
    @PostMapping("/{partNumber}/revisions/{revisionCode}/files")
    public List<PartAttachmentItemResponse> attachFiles(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        AttachPartFilesResult result = attachPartFilesUseCase.execute(
                new AttachPartFilesCommand(partNumber, revisionCode, request.fileIds())
        );
        List<UUID> attachedFileIds = result.fileIds();
        return partQuery.getFiles(new FileItemsCondition(attachedFileIds)).stream()
                .map(this::toPartAttachmentItemResponse)
                .toList();
    }

    @Operation(
            summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/files/{fileId}",
            description = "Part에 연결된 첨부파일 1건을 제거(소프트 삭제)합니다"
    )
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/files/{fileId}")
    public ResponseEntity<Void> detachFile(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable UUID fileId
    ) {
        detachPartFileUseCase.execute(new DetachPartFileCommand(partNumber, revisionCode, fileId));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/drawings/{drawingId}",
            description = "Part에 연결된 도면 1건을 삭제합니다 (Drawing + 연결 파일 soft delete)"
    )
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/drawings/{drawingId}")
    public ResponseEntity<Void> deleteDrawingFromPart(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable UUID drawingId
    ) {
        deletePartDrawingUseCase.execute(new DeletePartDrawingCommand(partNumber, revisionCode, drawingId));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/drawings",
            description = "업로드 완료 파일을 Drawing으로 등록하고 Part에 연결합니다"
    )
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drawings")
    public RegisterDrawingResponse registerDrawingForPart(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @Valid @RequestBody RegisterDrawingRequest request
    ) {
        RegisterPartDrawingResult result = registerPartDrawingUseCase.execute(
                new RegisterPartDrawingCommand(partNumber, revisionCode, request.fileId())
        );
        return new RegisterDrawingResponse(
                result.drawingId(),
                result.drawingNumber(),
                result.name()
        );
    }

    @Operation(
            summary = "PATCH /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview",
            description = "Part 대표 미리보기 소스를 파일 또는 도면으로 변경합니다"
    )
    @PatchMapping("/{partNumber}/revisions/{revisionCode}/preview")
    public PartPreviewResponse updatePreview(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @Valid @RequestBody ChangePartPreviewRequest request
    ) {
        changePartPreviewUseCase.execute(new ChangePartPreviewCommand(
                partNumber,
                revisionCode,
                request.sourceType(),
                request.sourceId()
        ));
        return toPartPreviewResponse(partQuery.get(new PartDetailCondition(partNumber, revisionCode)).preview());
    }

    @Operation(
            summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview/processing",
            description = "Part 대표 미리보기 비동기 처리 상태와 산출물 준비 여부를 조회합니다"
    )
    @GetMapping("/{partNumber}/revisions/{revisionCode}/preview/processing")
    public PartPreviewProcessingResponse getPreviewProcessing(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartPreviewProcessingResponse(
                partPreviewProcessingQuery.get(new PartPreviewProcessingCondition(partNumber, revisionCode))
        );
    }

    @Operation(
            summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview",
            description = "Part 대표 미리보기를 해제합니다"
    )
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/preview")
    public ResponseEntity<Void> deletePreview(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        clearPartPreviewUseCase.execute(new ClearPartPreviewCommand(partNumber, revisionCode));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "PATCH /api/v1/parts/categories/{category}",
            description = "카테고리 이름을 일괄 변경하고 변경 건수를 반환합니다"
    )
    @PatchMapping("/categories/{category}")
    public RenameCategoryResponse renameCategory(
            @PathVariable String category,
            @Valid @RequestBody RenameCategoryRequest request
    ) {
        RenameCategoryResult result = renameCategoryUseCase.execute(
                new RenameCategoryCommand(category, request.newName())
        );
        return new RenameCategoryResponse(result.updatedCount());
    }

    private PartLookupResponse toPartLookupResponse(PartLookupResult result) {
        return new PartLookupResponse(
                result.items().stream()
                        .map(item -> new PartLookupItemResponse(item.id(), item.partNumber(), item.name()))
                        .toList()
        );
    }

    private CategoryStatsResponse toCategoryStatsResponse(CategoryStatsResult result) {
        return new CategoryStatsResponse(
                result.items().stream()
                        .map(item -> new CategoryStatsItemResponse(item.category(), item.partCount()))
                        .toList()
        );
    }

    private CategoryLookupResponse toCategoryLookupResponse(CategoryLookupResult result) {
        return new CategoryLookupResponse(result.items());
    }

    private PartFilterOptionsResponse toPartFilterOptionsResponse(PartFilterOptionsResult result) {
        return new PartFilterOptionsResponse(result.categories(), result.lifecycleStates());
    }

    private PartListResponse toPartListResponse(PartListResult result) {
        return new PartListResponse(
                result.total(),
                result.offset(),
                result.limit(),
                result.items().stream()
                        .map(item -> new PartSummaryResponse(
                                item.id(),
                                item.partNumber(),
                                item.name(),
                                item.category(),
                                item.revision(),
                                item.lifecycleState(),
                                item.hasDrawing(),
                                item.childrenCount()
                        ))
                        .toList()
        );
    }

    private PartDetailResponse toPartDetailResponse(PartDetailResult result) {
        return new PartDetailResponse(
                result.id(),
                result.revisionId(),
                result.partNumber(),
                result.name(),
                result.revision(),
                result.material(),
                result.unit(),
                result.description(),
                result.category(),
                result.lifecycleState(),
                result.isPhantom(),
                result.leadTimeDays(),
                result.extendedProperties(),
                result.ownerId(),
                toPartOwnerUserSummaryResponse(result.owner()),
                result.ownerTeamId(),
                result.ownerTeamName(),
                toPartPreviewResponse(result.preview()),
                result.childrenCount(),
                result.parentsCount(),
                result.suppliersCount(),
                result.filesCount(),
                result.projectsCount()
        );
    }

    private PartBomResponse toPartBomResponse(PartBomResult result) {
        return new PartBomResponse(
                result.children().stream()
                        .map(item -> new BomChildResponse(
                                item.id(),
                                item.partNumber(),
                                item.name(),
                                item.quantity(),
                                item.extendedProperties()
                        ))
                        .toList(),
                result.parents().stream()
                        .map(item -> new BomParentResponse(
                                item.id(),
                                item.partNumber(),
                                item.name(),
                                item.quantity(),
                                item.extendedProperties()
                        ))
                        .toList()
        );
    }

    private BomTreeResponse toBomTreeResponse(BomTreeResult result) {
        return new BomTreeResponse(
                toBomTreeNodeResponse(result.root()),
                result.direction(),
                result.totalCount()
        );
    }

    private BomTreeNodeResponse toBomTreeNodeResponse(BomTreeResult.Node node) {
        return new BomTreeNodeResponse(
                node.id(),
                node.partNumber(),
                node.name(),
                node.revision(),
                node.material(),
                node.unit(),
                node.category(),
                node.lifecycleState(),
                node.quantity(),
                node.children().stream().map(this::toBomTreeNodeResponse).toList()
        );
    }

    private PartProjectsResponse toPartProjectsResponse(PartProjectsResult result) {
        return new PartProjectsResponse(
                result.total(),
                result.items().stream()
                        .map(item -> new PartProjectSummaryResponse(item.id(), item.name(), item.description()))
                        .toList()
        );
    }

    private PartFilesResponse toPartFilesResponse(PartFilesResult result) {
        return new PartFilesResponse(
                result.total(),
                result.items().stream().map(this::toPartAttachmentItemResponse).toList()
        );
    }

    private PartAttachmentItemResponse toPartAttachmentItemResponse(PartFilesResult.Item item) {
        return new PartAttachmentItemResponse(
                item.attachmentType(),
                item.fileId(),
                item.drawingId(),
                item.originalName(),
                item.contentType(),
                item.fileSize(),
                item.fileUrl(),
                item.previewSelectable(),
                item.selectedAsPreview(),
                item.createdAt()
        );
    }

    private PartSuppliersResponse toPartSuppliersResponse(PartSuppliersResult result) {
        return new PartSuppliersResponse(
                result.total(),
                result.items().stream()
                        .map(item -> new RelatedSupplierResponse(
                                item.id(),
                                item.companyName(),
                                item.code(),
                                item.country(),
                                item.unitCost()
                        ))
                        .toList()
        );
    }

    private PartOwnerUserSummaryResponse toPartOwnerUserSummaryResponse(
            com.fabbitinc.server.application.part.query.result.PartUserSummaryResult result
    ) {
        if (result == null) {
            return null;
        }
        return new PartOwnerUserSummaryResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl()
        );
    }

    private PartPreviewResponse toPartPreviewResponse(
            com.fabbitinc.server.application.part.query.result.PartPreviewResult result
    ) {
        if (result == null) {
            return null;
        }
        return new PartPreviewResponse(
                result.id(),
                result.sourceType(),
                result.sourceId(),
                result.processingStatus(),
                result.viewerType(),
                result.viewerUrl(),
                result.previewUrl(),
                result.originalFileUrl()
        );
    }

    private PartPreviewProcessingResponse toPartPreviewProcessingResponse(
            com.fabbitinc.server.application.part.query.result.PartPreviewProcessingResult result
    ) {
        return new PartPreviewProcessingResponse(
                result.status(),
                result.failureCode(),
                result.failureMessage(),
                result.pdfReady(),
                result.webpReady(),
                result.glbReady()
        );
    }

}
