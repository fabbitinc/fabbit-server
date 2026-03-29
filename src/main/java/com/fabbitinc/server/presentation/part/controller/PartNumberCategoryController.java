package com.fabbitinc.server.presentation.part.controller;

import com.fabbitinc.server.application.part.query.PartNumberCategoryQuery;
import com.fabbitinc.server.application.part.query.result.PartNumberCategoryListResult;
import com.fabbitinc.server.application.part.usecase.CreatePartNumberCategoryUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartNumberCategoryUseCase;
import com.fabbitinc.server.application.part.usecase.UpdatePartNumberCategoryUseCase;
import com.fabbitinc.server.application.part.usecase.command.CreatePartNumberCategoryCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartNumberCategoryCommand;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartNumberCategoryCommand;
import com.fabbitinc.server.application.part.usecase.result.PartNumberCategoryResult;
import com.fabbitinc.server.presentation.part.request.CreatePartNumberCategoryRequest;
import com.fabbitinc.server.presentation.part.request.UpdatePartNumberCategoryRequest;
import com.fabbitinc.server.presentation.part.response.PartNumberAvailabilityResponse;
import com.fabbitinc.server.presentation.part.response.PartNumberCategoryListResponse;
import com.fabbitinc.server.presentation.part.response.PartNumberCategoryResponse;
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
@RequestMapping("/api/v1/part-number-categories")
@Tag(name = "part-number-categories", description = "채번 카테고리 관리 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartNumberCategoryController {

    private final PartNumberCategoryQuery partNumberCategoryQuery;
    private final CreatePartNumberCategoryUseCase createPartNumberCategoryUseCase;
    private final UpdatePartNumberCategoryUseCase updatePartNumberCategoryUseCase;
    private final DeletePartNumberCategoryUseCase deletePartNumberCategoryUseCase;

    @Operation(summary = "채번 카테고리 목록을 조회합니다", description = "품번 생성 규칙으로 사용하는 채번 카테고리 목록과 예시 품번을 반환합니다")
    @ApiResponse(responseCode = "200", description = "요청 성공")
    @GetMapping
    public PartNumberCategoryListResponse list() {
        return toListResponse(partNumberCategoryQuery.list());
    }

    @Operation(summary = "채번 카테고리를 생성합니다", description = "새 채번 카테고리를 생성하고 시퀀스를 초기화합니다")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartNumberCategoryResponse create(@Valid @RequestBody CreatePartNumberCategoryRequest request) {
        return toResponse(createPartNumberCategoryUseCase.execute(new CreatePartNumberCategoryCommand(
                request.name(),
                request.prefix(),
                request.delimiter(),
                request.digits()
        )));
    }

    @Operation(summary = "채번 카테고리를 수정합니다", description = "기존 채번 카테고리의 이름, 접두어, 구분자, 자릿수를 수정합니다")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @PutMapping("/{categoryId}")
    public PartNumberCategoryResponse update(
            @Parameter(description = "채번 카테고리 ID") @PathVariable UUID categoryId,
            @Valid @RequestBody UpdatePartNumberCategoryRequest request
    ) {
        return toResponse(updatePartNumberCategoryUseCase.execute(new UpdatePartNumberCategoryCommand(
                categoryId,
                request.name(),
                request.prefix(),
                request.delimiter(),
                request.digits()
        )));
    }

    @Operation(summary = "다음 품번을 미리봅니다", description = "지정한 채번 카테고리 기준으로 다음에 생성될 예상 품번을 반환합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{categoryId}/next-number")
    public PartNumberPreviewResponse getNextNumber(
            @Parameter(description = "채번 카테고리 ID") @PathVariable UUID categoryId
    ) {
        var result = partNumberCategoryQuery.get(categoryId);
        return new PartNumberPreviewResponse(result.partNumber(), result.note());
    }

    @Operation(summary = "품번 중복 여부를 확인합니다", description = "입력한 품번이 현재 사용 가능한지 여부를 반환합니다")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/check-number")
    public PartNumberAvailabilityResponse checkNumber(
            @Parameter(description = "확인할 품번") @RequestParam String partNumber
    ) {
        var result = partNumberCategoryQuery.lookup(partNumber);
        return new PartNumberAvailabilityResponse(result.partNumber(), result.available());
    }

    @Operation(summary = "채번 카테고리를 삭제합니다", description = "사용 중이지 않은 채번 카테고리를 삭제합니다")
    @ApiResponse(responseCode = "204", description = "삭제 성공")
    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "채번 카테고리 ID") @PathVariable UUID categoryId) {
        deletePartNumberCategoryUseCase.execute(new DeletePartNumberCategoryCommand(categoryId));
    }

    private PartNumberCategoryListResponse toListResponse(PartNumberCategoryListResult result) {
        return new PartNumberCategoryListResponse(
                result.items().stream()
                        .map(item -> new PartNumberCategoryResponse(
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

    private PartNumberCategoryResponse toResponse(PartNumberCategoryResult result) {
        return new PartNumberCategoryResponse(
                result.id(),
                result.name(),
                result.prefix(),
                result.delimiter(),
                result.digits(),
                result.previewPartNumber()
        );
    }
}
