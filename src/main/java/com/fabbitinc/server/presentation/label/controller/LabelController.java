package com.fabbitinc.server.presentation.label.controller;

import com.fabbitinc.server.presentation.label.dto.request.CreateLabelRequest;
import com.fabbitinc.server.presentation.label.dto.request.UpdateLabelRequest;
import com.fabbitinc.server.presentation.label.dto.response.LabelListResponse;
import com.fabbitinc.server.presentation.label.dto.response.LabelLookupItemResponse;
import com.fabbitinc.server.presentation.label.dto.response.LabelLookupResponse;
import com.fabbitinc.server.presentation.label.dto.response.LabelResponse;
import com.fabbitinc.server.application.label.query.LabelQuery;
import com.fabbitinc.server.application.label.query.condition.LabelListCondition;
import com.fabbitinc.server.application.label.query.condition.LabelLookupCondition;
import com.fabbitinc.server.application.label.query.result.LabelListResult;
import com.fabbitinc.server.application.label.query.result.LabelLookupItemResult;
import com.fabbitinc.server.application.label.query.result.LabelLookupResult;
import com.fabbitinc.server.application.label.query.result.LabelResult;
import com.fabbitinc.server.application.label.usecase.CreateLabelUseCase;
import com.fabbitinc.server.application.label.usecase.DeleteLabelUseCase;
import com.fabbitinc.server.application.label.usecase.UpdateLabelUseCase;
import com.fabbitinc.server.application.label.usecase.command.CreateLabelCommand;
import com.fabbitinc.server.application.label.usecase.command.DeleteLabelCommand;
import com.fabbitinc.server.application.label.usecase.command.UpdateLabelCommand;
import com.fabbitinc.server.application.label.usecase.result.CreateLabelResult;
import com.fabbitinc.server.application.label.usecase.result.UpdateLabelResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/labels")
@Tag(name = "labels", description = "라벨 조회/생성/수정/삭제 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class LabelController {

    private final LabelQuery labelQuery;
    private final CreateLabelUseCase createLabelUseCase;
    private final UpdateLabelUseCase updateLabelUseCase;
    private final DeleteLabelUseCase deleteLabelUseCase;

    @Operation(
            summary = "라벨 picker/autocomplete 용 경량 목록(id, name, color)을 조회합니다",
            description = "라벨 picker/autocomplete 용 경량 목록(id, name, color)을 조회합니다"
    )
    @GetMapping("/lookup")
    public LabelLookupResponse lookupLabels(
            @Parameter(description = "라벨 이름 검색어", example = "긴급")
            @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "조회 건수", example = "10")
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        return toLabelLookupResponse(labelQuery.lookup(new LabelLookupCondition(search, limit)));
    }

    @Operation(
            summary = "테넌트에 등록된 전체 라벨 목록을 이름순으로 조회합니다",
            description = "테넌트에 등록된 전체 라벨 목록을 이름순으로 조회합니다"
    )
    @GetMapping
    public LabelListResponse listLabels() {
        return toLabelListResponse(labelQuery.list(new LabelListCondition()));
    }

    @Operation(
            summary = "라벨을 생성합니다. 동일한 라벨 이름은 허용되지 않습니다",
            description = "라벨을 생성합니다. 동일한 라벨 이름은 허용되지 않습니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabelResponse createLabel(
            @Parameter(description = "라벨 생성 요청")
            @Valid @RequestBody CreateLabelRequest request
    ) {
        CreateLabelResult result = createLabelUseCase.execute(
                new CreateLabelCommand(request.name(), request.description(), request.color())
        );
        return toLabelResponse(result);
    }

    @Operation(
            summary = "라벨의 일부 필드를 수정합니다. description을 null로 보내면 설명이 제거됩니다",
            description = "라벨의 일부 필드를 수정합니다. description을 null로 보내면 설명이 제거됩니다"
    )
    @PatchMapping("/{labelId}")
    public LabelResponse updateLabel(
            @Parameter(description = "수정할 라벨 ID")
            @PathVariable UUID labelId,
            @Parameter(description = "라벨 수정 요청")
            @Valid @RequestBody UpdateLabelRequest request
    ) {
        UpdateLabelResult result = updateLabelUseCase.execute(
                new UpdateLabelCommand(
                        labelId,
                        request.getName(),
                        request.getDescription(),
                        request.getColor(),
                        request.isDescriptionSet()
                )
        );
        return toLabelResponse(result);
    }

    @Operation(
            summary = "라벨을 삭제합니다",
            description = "라벨을 삭제합니다"
    )
    @DeleteMapping("/{labelId}")
    public ResponseEntity<Void> deleteLabel(
            @Parameter(description = "삭제할 라벨 ID")
            @PathVariable UUID labelId
    ) {
        deleteLabelUseCase.execute(new DeleteLabelCommand(labelId));
        return ResponseEntity.noContent().build();
    }

    private LabelListResponse toLabelListResponse(LabelListResult result) {
        return new LabelListResponse(
                result.total(),
                result.items().stream()
                        .map(this::toLabelResponse)
                        .toList()
        );
    }

    private LabelResponse toLabelResponse(LabelResult result) {
        return new LabelResponse(
                result.id(),
                result.name(),
                result.description(),
                result.color(),
                result.createdAt(),
                result.createdBy()
        );
    }

    private LabelResponse toLabelResponse(CreateLabelResult result) {
        return new LabelResponse(
                result.id(),
                result.name(),
                result.description(),
                result.color(),
                result.createdAt(),
                result.createdBy()
        );
    }

    private LabelResponse toLabelResponse(UpdateLabelResult result) {
        return new LabelResponse(
                result.id(),
                result.name(),
                result.description(),
                result.color(),
                result.createdAt(),
                result.createdBy()
        );
    }

    private LabelLookupResponse toLabelLookupResponse(LabelLookupResult result) {
        return new LabelLookupResponse(
                result.items().stream()
                        .map(this::toLabelLookupItemResponse)
                        .toList()
        );
    }

    private LabelLookupItemResponse toLabelLookupItemResponse(LabelLookupItemResult result) {
        return new LabelLookupItemResponse(
                result.id(),
                result.name(),
                result.color()
        );
    }
}
