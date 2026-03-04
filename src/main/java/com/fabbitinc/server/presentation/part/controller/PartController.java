package com.fabbitinc.server.presentation.part.controller;

import com.fabbitinc.server.application.file.dto.response.FileItemResponse;
import com.fabbitinc.server.application.drawing.dto.request.RegisterDrawingRequest;
import com.fabbitinc.server.application.drawing.dto.response.RegisterDrawingResponse;
import com.fabbitinc.server.application.part.dto.request.AttachFilesRequest;
import com.fabbitinc.server.application.part.dto.request.RenameCategoryRequest;
import com.fabbitinc.server.application.part.dto.response.CategoryLookupResponse;
import com.fabbitinc.server.application.part.dto.response.CategoryStatsResponse;
import com.fabbitinc.server.application.part.dto.response.BomTreeResponse;
import com.fabbitinc.server.application.part.dto.response.PartBomResponse;
import com.fabbitinc.server.application.part.dto.response.PartDetailResponse;
import com.fabbitinc.server.application.part.dto.response.PartFilterOptionsResponse;
import com.fabbitinc.server.application.part.dto.response.PartFilesResponse;
import com.fabbitinc.server.application.part.dto.response.PartListResponse;
import com.fabbitinc.server.application.part.dto.response.PartLookupResponse;
import com.fabbitinc.server.application.part.dto.response.PartSuppliersResponse;
import com.fabbitinc.server.application.part.dto.response.RenameCategoryResponse;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.usecase.AttachPartFilesUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.DetachPartFileUseCase;
import com.fabbitinc.server.application.part.usecase.RegisterPartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.RenameCategoryUseCase;
import com.fabbitinc.server.application.project.dto.response.PartProjectsResponse;
import com.fabbitinc.server.application.project.query.ProjectQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "parts", description = "부품 조회/카테고리 API")
public class PartController {

    private final PartQuery partQuery;
    private final RenameCategoryUseCase renameCategoryUseCase;
    private final AttachPartFilesUseCase attachPartFilesUseCase;
    private final DetachPartFileUseCase detachPartFileUseCase;
    private final RegisterPartDrawingUseCase registerPartDrawingUseCase;
    private final DeletePartDrawingUseCase deletePartDrawingUseCase;
    private final ProjectQuery projectQuery;

    @Operation(
            summary = "GET /api/v1/parts/lookup",
            description = "품번/품명으로 경량 Part 목록을 조회합니다"
    )
    @GetMapping("/lookup")
    public PartLookupResponse lookupParts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit
    ) {
        return partQuery.lookupParts(search, limit);
    }

    @Operation(
            summary = "GET /api/v1/parts/export",
            description = "필터링된 Part 목록을 Excel(.xlsx) 파일로 내보냅니다"
    )
    @GetMapping("/export")
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
        byte[] content = partQuery.exportPartsExcel(search,
                category,
                lifecycleState,
                hasDrawing,
                hasChildren,
                partIds,
                mappingId,
                projectId
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ));
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
        return partQuery.listCategories();
    }

    @Operation(
            summary = "GET /api/v1/parts/categories/lookup",
            description = "카테고리 문자열 목록을 경량 조회합니다"
    )
    @GetMapping("/categories/lookup")
    public CategoryLookupResponse lookupCategories(
) {
        return partQuery.lookupCategories();
    }

    @Operation(
            summary = "GET /api/v1/parts/filter-options",
            description = "Part 목록 필터 옵션(카테고리/수명주기 상태)을 조회합니다"
    )
    @GetMapping("/filter-options")
    public PartFilterOptionsResponse getFilterOptions(
) {
        return partQuery.getFilterOptions();
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
            @Min(value = 0, message = "offset은 0 이상이어야 합니다")
            int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 100, message = "limit은 100 이하여야 합니다")
            int limit
    ) {
        return partQuery.listParts(search,
                category,
                lifecycleState,
                hasDrawing,
                hasChildren,
                projectId,
                offset,
                limit
        );
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}",
            description = "Part 상세 정보와 관계 카운트(children/parents/suppliers/files/projects)를 조회합니다"
    )
    @GetMapping("/{partId}")
    public PartDetailResponse getPart(
            @PathVariable UUID partId
    ) {
        return partQuery.getPartDetail(partId);
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/bom",
            description = "Part의 직접 자식/직접 부모 BOM 관계(1-depth)를 조회합니다"
    )
    @GetMapping("/{partId}/bom")
    public PartBomResponse getPartBom(
            @PathVariable UUID partId
    ) {
        return partQuery.getPartBom(partId);
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/bom/tree",
            description = "Part BOM 트리를 정전개(forward) 또는 역전개(reverse)로 조회합니다"
    )
    @GetMapping("/{partId}/bom/tree")
    public BomTreeResponse getBomTree(
            @PathVariable UUID partId,
            @RequestParam(value = "direction", defaultValue = "forward") String direction
    ) {
        return partQuery.getBomTree(partId, direction);
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/bom/tree/export",
            description = "Part BOM 트리를 Excel(.xlsx) 파일로 내보냅니다"
    )
    @GetMapping("/{partId}/bom/tree/export")
    public ResponseEntity<byte[]> exportBomTree(
            @PathVariable UUID partId,
            @RequestParam(value = "direction", defaultValue = "forward") String direction,
            @RequestParam(value = "mapping_id", required = false) UUID mappingId
    ) {
        byte[] content = partQuery.exportBomTreeExcel(partId, direction, mappingId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ));
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
        return projectQuery.getPartProjects(partId);
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/files",
            description = "Part에 연결된 업로드 완료 파일 목록을 조회합니다"
    )
    @GetMapping("/{partId}/files")
    public PartFilesResponse getPartFiles(
            @PathVariable UUID partId
    ) {
        return partQuery.getPartFiles(partId);
    }

    @Operation(
            summary = "GET /api/v1/parts/{partId}/suppliers",
            description = "Part에 연결된 공급사 목록을 조회합니다"
    )
    @GetMapping("/{partId}/suppliers")
    public PartSuppliersResponse getPartSuppliers(
            @PathVariable UUID partId
    ) {
        return partQuery.getPartSuppliers(partId);
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
        List<UUID> attachedFileIds = attachPartFilesUseCase.execute(partId, request.fileIds());
        return partQuery.getFilesByIds(attachedFileIds);
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
        detachPartFileUseCase.execute(partId, fileId);
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
        deletePartDrawingUseCase.execute(partId);
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
        return registerPartDrawingUseCase.execute(partId, request.fileId());
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
        int updatedCount = renameCategoryUseCase.execute(category, request.newName());
        return new RenameCategoryResponse(updatedCount);
    }
}
