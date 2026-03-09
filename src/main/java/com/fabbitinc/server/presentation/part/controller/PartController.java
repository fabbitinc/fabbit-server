package com.fabbitinc.server.presentation.part.controller;

import com.fabbitinc.server.application.drawing.dto.request.RegisterDrawingRequest;
import com.fabbitinc.server.application.drawing.dto.response.RegisterDrawingResponse;
import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import com.fabbitinc.server.application.part.dto.request.AttachFilesRequest;
import com.fabbitinc.server.application.part.dto.request.RenameCategoryRequest;
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
import com.fabbitinc.server.application.part.dto.response.PartSummaryResponse;
import com.fabbitinc.server.application.part.dto.response.PartSuppliersResponse;
import com.fabbitinc.server.application.part.dto.response.RelatedDrawingResponse;
import com.fabbitinc.server.application.part.dto.response.RelatedSupplierResponse;
import com.fabbitinc.server.application.part.dto.response.RenameCategoryResponse;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.BomTreeCondition;
import com.fabbitinc.server.application.part.query.condition.BomTreeExportCondition;
import com.fabbitinc.server.application.part.query.condition.FileItemsCondition;
import com.fabbitinc.server.application.part.query.condition.PartBomCondition;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartExportCondition;
import com.fabbitinc.server.application.part.query.condition.PartFilesCondition;
import com.fabbitinc.server.application.part.query.condition.PartListCondition;
import com.fabbitinc.server.application.part.query.condition.PartLookupCondition;
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
import com.fabbitinc.server.application.part.usecase.DeletePartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.DetachPartFileUseCase;
import com.fabbitinc.server.application.part.usecase.RegisterPartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.RenameCategoryUseCase;
import com.fabbitinc.server.application.part.usecase.command.AttachPartFilesCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.command.DetachPartFileCommand;
import com.fabbitinc.server.application.part.usecase.command.RegisterPartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.command.RenameCategoryCommand;
import com.fabbitinc.server.application.part.usecase.result.AttachPartFilesResult;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "parts", description = "부품 조회/카테고리 API")
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
    private final RenameCategoryUseCase renameCategoryUseCase;
    private final AttachPartFilesUseCase attachPartFilesUseCase;
    private final DetachPartFileUseCase detachPartFileUseCase;
    private final RegisterPartDrawingUseCase registerPartDrawingUseCase;
    private final DeletePartDrawingUseCase deletePartDrawingUseCase;

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
            summary = "GET /api/v1/parts/{partId}",
            description = "Part 상세 정보와 관계 카운트(children/parents/suppliers/files/projects)를 조회합니다"
    )
    @GetMapping("/{partId}")
    public PartDetailResponse getPart(
            @Parameter(description = "조회할 부품 ID")
            @PathVariable UUID partId
    ) {
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(partId)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/bom",
            description = "Part의 직접 자식/직접 부모 BOM 관계(1-depth)를 조회합니다"
    )
    @GetMapping("/{partId}/bom")
    public PartBomResponse getPartBom(
            @Parameter(description = "BOM을 조회할 부품 ID")
            @PathVariable UUID partId
    ) {
        return toPartBomResponse(partQuery.get(new PartBomCondition(partId)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/bom/tree",
            description = "Part BOM 트리를 정전개(forward) 또는 역전개(reverse)로 조회합니다"
    )
    @GetMapping("/{partId}/bom/tree")
    public BomTreeResponse getBomTree(
            @Parameter(description = "BOM 트리를 조회할 부품 ID")
            @PathVariable UUID partId,
            @Parameter(description = "정전개/역전개 방향", example = "FORWARD")
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction
    ) {
        return toBomTreeResponse(partQuery.getBomTree(new BomTreeCondition(partId, direction)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/bom/tree/export",
            description = "Part BOM 트리를 Excel(.xlsx) 파일로 내보냅니다"
    )
    @GetMapping(value = "/{partId}/bom/tree/export", produces = EXCEL_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportBomTree(
            @PathVariable UUID partId,
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction,
            @RequestParam(value = "mapping_id", required = false) UUID mappingId
    ) {
        byte[] content = partQuery.exportBomTree(new BomTreeExportCondition(partId, direction, mappingId));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BOM.xlsx");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/projects",
            description = "해당 Part가 소속된 프로젝트 목록을 조회합니다"
    )
    @GetMapping("/{partId}/projects")
    public PartProjectsResponse getPartProjects(
            @PathVariable UUID partId
    ) {
        return toPartProjectsResponse(partQuery.get(new PartProjectsCondition(partId)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/files",
            description = "Part에 연결된 업로드 완료 파일 목록을 조회합니다"
    )
    @GetMapping("/{partId}/files")
    public PartFilesResponse getPartFiles(
            @PathVariable UUID partId
    ) {
        return toPartFilesResponse(partQuery.get(new PartFilesCondition(partId)));
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/suppliers",
            description = "Part에 연결된 공급사 목록을 조회합니다"
    )
    @GetMapping("/{partId}/suppliers")
    public PartSuppliersResponse getPartSuppliers(
            @PathVariable UUID partId
    ) {
        return toPartSuppliersResponse(partQuery.get(new PartSuppliersCondition(partId)));
    }

    @Operation(
            summary = "POST /api/v1/parts/{partId}/files",
            description = "업로드 완료 파일들을 Part에 배치 연결합니다"
    )
    @PostMapping("/{partId}/files")
    public List<FileItemResponse> attachFiles(
            @PathVariable UUID partId,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        AttachPartFilesResult result = attachPartFilesUseCase.execute(
                new AttachPartFilesCommand(partId, request.fileIds())
        );
        List<UUID> attachedFileIds = result.fileIds();
        return partQuery.getFiles(new FileItemsCondition(attachedFileIds)).stream()
                .map(this::toFileItemResponse)
                .toList();
    }

    @Operation(
            summary = "DELETE /api/v1/parts/{partId}/files/{fileId}",
            description = "Part에 연결된 첨부파일 1건을 제거(소프트 삭제)합니다"
    )
    @DeleteMapping("/{partId}/files/{fileId}")
    public ResponseEntity<Void> detachFile(
            @PathVariable UUID partId,
            @PathVariable UUID fileId
    ) {
        detachPartFileUseCase.execute(new DetachPartFileCommand(partId, fileId));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "DELETE /api/v1/parts/{partId}/drawings",
            description = "Part에 연결된 도면을 삭제합니다 (Drawing + 연결 파일 soft delete)"
    )
    @DeleteMapping("/{partId}/drawings")
    public ResponseEntity<Void> deleteDrawingFromPart(
            @PathVariable UUID partId
    ) {
        deletePartDrawingUseCase.execute(new DeletePartDrawingCommand(partId));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "POST /api/v1/parts/{partId}/drawings",
            description = "업로드 완료 파일을 Drawing으로 등록하고 Part에 연결합니다"
    )
    @PostMapping("/{partId}/drawings")
    public RegisterDrawingResponse registerDrawingForPart(
            @PathVariable UUID partId,
            @Valid @RequestBody RegisterDrawingRequest request
    ) {
        RegisterPartDrawingResult result = registerPartDrawingUseCase.execute(
                new RegisterPartDrawingCommand(partId, request.fileId())
        );
        return new RegisterDrawingResponse(
                result.drawingId(),
                result.drawingNumber(),
                result.name(),
                result.conversionStatus()
        );
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
                                item.drawingId(),
                                item.childrenCount()
                        ))
                        .toList()
        );
    }

    private PartDetailResponse toPartDetailResponse(PartDetailResult result) {
        return new PartDetailResponse(
                result.id(),
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
                toRelatedDrawingResponse(result.drawing()),
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
                result.items().stream().map(this::toFileItemResponse).toList()
        );
    }

    private FileItemResponse toFileItemResponse(PartFilesResult.Item item) {
        return new FileItemResponse(
                item.fileId(),
                item.originalName(),
                item.contentType(),
                item.fileSize(),
                item.fileUrl(),
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

    private RelatedDrawingResponse toRelatedDrawingResponse(
            com.fabbitinc.server.application.part.query.result.RelatedDrawingResult result
    ) {
        if (result == null) {
            return null;
        }
        return new RelatedDrawingResponse(
                result.id(),
                result.drawingNumber(),
                result.name(),
                result.version(),
                result.status(),
                result.conversionStatus(),
                result.viewerType(),
                result.viewerUrl(),
                result.previewUrl(),
                result.originalFileUrl()
        );
    }

}
