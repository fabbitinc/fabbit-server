package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toCategoryLookupResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toCategoryStatsResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartDetailResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartFilterOptionsResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartListResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartLookupResponse;

import com.fabbitinc.server.application.part.dto.request.CreatePartRequest;
import com.fabbitinc.server.application.part.dto.request.RenameCategoryRequest;
import com.fabbitinc.server.application.part.dto.response.CategoryLookupResponse;
import com.fabbitinc.server.application.part.dto.response.CategoryStatsResponse;
import com.fabbitinc.server.application.part.dto.response.PartDetailResponse;
import com.fabbitinc.server.application.part.dto.response.PartFilterOptionsResponse;
import com.fabbitinc.server.application.part.dto.response.PartListResponse;
import com.fabbitinc.server.application.part.dto.response.PartLookupResponse;
import com.fabbitinc.server.application.part.dto.response.RenameCategoryResponse;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.PartDraftDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartExportCondition;
import com.fabbitinc.server.application.part.query.condition.PartListCondition;
import com.fabbitinc.server.application.part.query.condition.PartLookupCondition;
import com.fabbitinc.server.application.part.usecase.CreatePartUseCase;
import com.fabbitinc.server.application.part.usecase.RenameCategoryUseCase;
import com.fabbitinc.server.application.part.usecase.command.CreatePartCommand;
import com.fabbitinc.server.application.part.usecase.command.RenameCategoryCommand;
import com.fabbitinc.server.application.part.usecase.result.CreatePartResult;
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
    private final CreatePartUseCase createPartUseCase;
    private final RenameCategoryUseCase renameCategoryUseCase;

    @Operation(
            summary = "POST /api/v1/parts",
            description = "부품을 생성하고 생성 직후 상세 정보를 반환합니다"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
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
                request.extendedProperties(),
                request.reason()
        ));
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(
                result.partNumber(),
                null,
                result.draftKey()
        )));
    }

    @Operation(summary = "GET /api/v1/parts/lookup", description = "품번/품명으로 경량 Part 목록을 조회합니다")
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

    @Operation(summary = "GET /api/v1/parts/export", description = "필터링된 Part 목록을 Excel(.xlsx) 파일로 내보냅니다")
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

    @Operation(summary = "GET /api/v1/parts/categories", description = "카테고리별 부품 개수를 조회합니다")
    @GetMapping("/categories")
    public CategoryStatsResponse listCategories() {
        return toCategoryStatsResponse(partQuery.listCategories());
    }

    @Operation(summary = "GET /api/v1/parts/categories/lookup", description = "카테고리 문자열 목록을 경량 조회합니다")
    @GetMapping("/categories/lookup")
    public CategoryLookupResponse lookupCategories() {
        return toCategoryLookupResponse(partQuery.lookupCategories());
    }

    @Operation(summary = "GET /api/v1/parts/filter-options", description = "Part 목록 필터 옵션(카테고리/수명주기 상태)을 조회합니다")
    @GetMapping("/filter-options")
    public PartFilterOptionsResponse getFilterOptions() {
        return toPartFilterOptionsResponse(partQuery.getFilterOptions());
    }

    @Operation(summary = "GET /api/v1/parts", description = "Part 목록을 검색/필터 조건과 함께 조회합니다")
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

    @Operation(summary = "PATCH /api/v1/parts/categories/{category}", description = "카테고리 이름을 일괄 변경하고 변경 건수를 반환합니다")
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
}
