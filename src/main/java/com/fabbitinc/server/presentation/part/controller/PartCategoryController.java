package com.fabbitinc.server.presentation.part.controller;

import com.fabbitinc.server.application.part.query.PartCategoryQuery;
import com.fabbitinc.server.application.part.query.result.PartCategoryListResult;
import com.fabbitinc.server.application.part.usecase.CreatePartCategoryUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartCategoryUseCase;
import com.fabbitinc.server.application.part.usecase.UpdatePartCategoryUseCase;
import com.fabbitinc.server.application.part.usecase.command.CreatePartCategoryCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartCategoryCommand;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartCategoryCommand;
import com.fabbitinc.server.application.part.usecase.result.PartCategoryResult;
import com.fabbitinc.server.presentation.part.request.CreatePartCategoryRequest;
import com.fabbitinc.server.presentation.part.request.UpdatePartCategoryRequest;
import com.fabbitinc.server.presentation.part.response.PartCategoryListResponse;
import com.fabbitinc.server.presentation.part.response.PartCategoryResponse;
import com.fabbitinc.server.presentation.part.response.PartNumberAvailabilityResponse;
import com.fabbitinc.server.presentation.part.response.PartNumberPreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/part-categories")
@Tag(name = "part-categories", description = "부품 카테고리 관리 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartCategoryController {

    private final PartCategoryQuery partCategoryQuery;
    private final CreatePartCategoryUseCase createPartCategoryUseCase;
    private final UpdatePartCategoryUseCase updatePartCategoryUseCase;
    private final DeletePartCategoryUseCase deletePartCategoryUseCase;

    @Operation(operationId = "partCategoryList", summary = "부품 카테고리 목록을 조회합니다", description = "부품 카테고리 목록과 예시 품번을 반환합니다")
    @ApiResponse(responseCode = "200", description = "요청 성공")
    @GetMapping
    public PartCategoryListResponse list() {
        return toListResponse(partCategoryQuery.list());
    }

    @Operation(operationId = "partCategoryCreate", summary = "부품 카테고리를 생성합니다", description = "새 부품 카테고리를 생성하고 시퀀스를 초기화합니다")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartCategoryResponse create(@Valid @RequestBody CreatePartCategoryRequest request) {
        return toResponse(createPartCategoryUseCase.execute(new CreatePartCategoryCommand(
                request.name(),
                request.prefix(),
                request.delimiter(),
                request.digits()
        )));
    }

    @Operation(operationId = "partCategoryUpdate", summary = "부품 카테고리를 수정합니다", description = "기존 부품 카테고리의 이름, itemType, 접두어, 구분자, 자릿수를 수정합니다")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @PutMapping("/{categoryId}")
    public PartCategoryResponse update(
            @Parameter(description = "카테고리 ID") @PathVariable UUID categoryId,
            @Valid @RequestBody UpdatePartCategoryRequest request
    ) {
        return toResponse(updatePartCategoryUseCase.execute(new UpdatePartCategoryCommand(
                categoryId,
                request.name(),
                request.prefix(),
                request.delimiter(),
                request.digits()
        )));
    }

    @Operation(operationId = "partCategoryGetNextNumber", summary = "다음 품번을 미리봅니다", description = "지정한 카테고리 기준으로 다음에 생성될 예상 품번을 반환합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{categoryId}/next-number")
    public PartNumberPreviewResponse getNextNumber(
            @Parameter(description = "카테고리 ID") @PathVariable UUID categoryId
    ) {
        var result = partCategoryQuery.get(categoryId);
        return new PartNumberPreviewResponse(result.partNumber(), result.note());
    }

    @Operation(operationId = "partCategoryCheckNumber", summary = "품번 중복 여부를 확인합니다", description = "입력한 품번이 현재 사용 가능한지 여부를 반환합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/check-number")
    public PartNumberAvailabilityResponse checkNumber(
            @Parameter(description = "확인할 품번") @RequestParam String partNumber
    ) {
        var result = partCategoryQuery.lookup(partNumber);
        return new PartNumberAvailabilityResponse(result.partNumber(), result.available());
    }

    @Operation(operationId = "partCategoryDelete", summary = "부품 카테고리를 삭제합니다", description = "사용 중이지 않은 부품 카테고리를 삭제합니다")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "카테고리 ID") @PathVariable UUID categoryId) {
        deletePartCategoryUseCase.execute(new DeletePartCategoryCommand(categoryId));
    }

    private PartCategoryListResponse toListResponse(PartCategoryListResult result) {
        return new PartCategoryListResponse(
                result.items().stream()
                        .map(item -> new PartCategoryResponse(
                                item.id(),
                                item.name(),
                                item.prefix(),
                                item.delimiter(),
                                item.digits(),
                                item.previewPartNumber()
                        ))
                        .toList()
        );
    }

    private PartCategoryResponse toResponse(PartCategoryResult result) {
        return new PartCategoryResponse(
                result.id(),
                result.name(),
                result.prefix(),
                result.delimiter(),
                result.digits(),
                result.previewPartNumber()
        );
    }
}
