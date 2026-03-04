package com.fabbitinc.server.presentation.mapping.controller;

import com.fabbitinc.server.application.mapping.dto.request.MappingConfirmRequest;
import com.fabbitinc.server.application.mapping.dto.request.MappingPreviewRequest;
import com.fabbitinc.server.application.mapping.dto.request.MappingUpdateRequest;
import com.fabbitinc.server.application.mapping.dto.request.MappingValidateRequest;
import com.fabbitinc.server.application.mapping.dto.response.MappingListResponse;
import com.fabbitinc.server.application.mapping.dto.response.MappingPreviewResponse;
import com.fabbitinc.server.application.mapping.dto.response.MappingResponse;
import com.fabbitinc.server.application.mapping.dto.response.MappingValidateResponse;
import com.fabbitinc.server.application.mapping.query.MappingQuery;
import com.fabbitinc.server.application.mapping.usecase.ConfirmMappingUseCase;
import com.fabbitinc.server.application.mapping.usecase.DeactivateMappingUseCase;
import com.fabbitinc.server.application.mapping.usecase.PreviewMappingUseCase;
import com.fabbitinc.server.application.mapping.usecase.UpdateMappingUseCase;
import com.fabbitinc.server.application.mapping.usecase.ValidateMappingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mappings")
@Tag(name = "mappings", description = "매핑 생성/검증/조회 API")
public class MappingController {

    private final PreviewMappingUseCase previewMappingUseCase;
    private final ConfirmMappingUseCase confirmMappingUseCase;
    private final ValidateMappingUseCase validateMappingUseCase;
    private final UpdateMappingUseCase updateMappingUseCase;
    private final DeactivateMappingUseCase deactivateMappingUseCase;
    private final MappingQuery mappingQuery;

    @Operation(
            summary = "POST /api/v1/mappings/preview",
            description = "업로드된 파일의 헤더/샘플 행을 기반으로 매핑 미리보기를 생성합니다"
    )
    @PostMapping("/preview")
    public MappingPreviewResponse preview(
            @Valid @RequestBody MappingPreviewRequest request
    ) {
        return previewMappingUseCase.execute(request);
    }

    @Operation(
            summary = "POST /api/v1/mappings/confirm",
            description = "검토된 매핑을 확정하여 새 매핑 레코드(버전 1)를 생성합니다"
    )
    @PostMapping("/confirm")
    public MappingResponse confirm(@Valid @RequestBody MappingConfirmRequest request) {
        return confirmMappingUseCase.execute(request);
    }

    @Operation(
            summary = "POST /api/v1/mappings/validate",
            description = "매핑을 정규화하고 파일 샘플 데이터 기준으로 오류/경고를 검증합니다"
    )
    @PostMapping("/validate")
    public MappingValidateResponse validate(@Valid @RequestBody MappingValidateRequest request) {
        return validateMappingUseCase.execute(request);
    }

    @Operation(
            summary = "GET /api/v1/mappings",
            description = "활성 매핑 목록을 최신순으로 조회합니다"
    )
    @GetMapping
    public MappingListResponse list() {
        return mappingQuery.listMappings();
    }

    @Operation(
            summary = "GET /api/v1/mappings/{mappingId}",
            description = "매핑 ID로 최신 리비전을 조회합니다"
    )
    @GetMapping("/{mappingId}")
    public MappingResponse get(@PathVariable UUID mappingId) {
        return mappingQuery.getMapping(mappingId);
    }

    @Operation(
            summary = "PUT /api/v1/mappings/{mappingId}",
            description = "매핑을 수정하고 새로운 리비전을 생성합니다"
    )
    @PutMapping("/{mappingId}")
    public MappingResponse update(
            @PathVariable UUID mappingId,
            @Valid @RequestBody MappingUpdateRequest request
    ) {
        return updateMappingUseCase.execute(mappingId, request);
    }

    @Operation(
            summary = "DELETE /api/v1/mappings/{mappingId}",
            description = "매핑을 비활성화(soft delete)합니다"
    )
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<Void> delete(@PathVariable UUID mappingId) {
        deactivateMappingUseCase.execute(mappingId);
        return ResponseEntity.noContent().build();
    }
}
