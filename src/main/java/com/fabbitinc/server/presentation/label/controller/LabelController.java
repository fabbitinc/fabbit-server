package com.fabbitinc.server.presentation.label.controller;

import com.fabbitinc.server.application.label.dto.request.CreateLabelRequest;
import com.fabbitinc.server.application.label.dto.request.UpdateLabelRequest;
import com.fabbitinc.server.application.label.dto.response.LabelListResponse;
import com.fabbitinc.server.application.label.dto.response.LabelLookupResponse;
import com.fabbitinc.server.application.label.dto.response.LabelResponse;
import com.fabbitinc.server.application.label.query.LabelQuery;
import com.fabbitinc.server.application.label.usecase.CreateLabelUseCase;
import com.fabbitinc.server.application.label.usecase.DeleteLabelUseCase;
import com.fabbitinc.server.application.label.usecase.UpdateLabelUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/labels")
@Tag(name = "labels", description = "라벨 조회/생성/수정/삭제 API")
public class LabelController {

    private final LabelQuery labelQuery;
    private final CreateLabelUseCase createLabelUseCase;
    private final UpdateLabelUseCase updateLabelUseCase;
    private final DeleteLabelUseCase deleteLabelUseCase;

    @Operation(
            summary = "GET /api/v1/labels/lookup",
            description = "라벨 picker/autocomplete 용 경량 목록(id, name, color)을 조회합니다"
    )
    @GetMapping("/lookup")
    public LabelLookupResponse lookupLabels(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit
    ) {
        return labelQuery.lookupLabels(search, limit);
    }

    @Operation(
            summary = "GET /api/v1/labels",
            description = "테넌트에 등록된 전체 라벨 목록을 이름순으로 조회합니다"
    )
    @GetMapping
    public LabelListResponse listLabels() {
        return labelQuery.listLabels();
    }

    @Operation(
            summary = "POST /api/v1/labels",
            description = "라벨을 생성합니다. 동일한 라벨 이름은 허용되지 않습니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabelResponse createLabel(
            @Valid @RequestBody CreateLabelRequest request
    ) {
        return createLabelUseCase.execute(request);
    }

    @Operation(
            summary = "PATCH /api/v1/labels/{labelId}",
            description = "라벨의 일부 필드를 수정합니다. description을 null로 보내면 설명이 제거됩니다"
    )
    @PatchMapping("/{labelId}")
    public LabelResponse updateLabel(
            @PathVariable UUID labelId,
            @Valid @RequestBody UpdateLabelRequest request
    ) {
        return updateLabelUseCase.execute(labelId, request);
    }

    @Operation(
            summary = "DELETE /api/v1/labels/{labelId}",
            description = "라벨을 삭제합니다"
    )
    @DeleteMapping("/{labelId}")
    public ResponseEntity<Void> deleteLabel(
            @PathVariable UUID labelId
    ) {
        deleteLabelUseCase.execute(labelId);
        return ResponseEntity.noContent().build();
    }
}
