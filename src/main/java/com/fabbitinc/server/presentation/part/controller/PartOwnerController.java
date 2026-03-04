package com.fabbitinc.server.presentation.part.controller;

import com.fabbitinc.server.application.part.dto.request.PartDefaultOwnerRequest;
import com.fabbitinc.server.application.part.dto.request.UpdatePartOwnerRequest;
import com.fabbitinc.server.application.part.dto.response.PartDefaultOwnerItemResponse;
import com.fabbitinc.server.application.part.dto.response.PartDefaultOwnerListResponse;
import com.fabbitinc.server.application.part.dto.response.PartOwnerResponse;
import com.fabbitinc.server.application.part.query.PartOwnerQuery;
import com.fabbitinc.server.application.part.usecase.DeleteDefaultOwnerUseCase;
import com.fabbitinc.server.application.part.usecase.UpdatePartOwnerUseCase;
import com.fabbitinc.server.application.part.usecase.UpsertDefaultOwnerUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "part-owner", description = "부품 담당자/기본 담당자 관리 API")
public class PartOwnerController {

    private final PartOwnerQuery partOwnerQuery;
    private final UpdatePartOwnerUseCase updatePartOwnerUseCase;
    private final UpsertDefaultOwnerUseCase upsertDefaultOwnerUseCase;
    private final DeleteDefaultOwnerUseCase deleteDefaultOwnerUseCase;

    @Operation(
            summary = "GET /api/v1/parts/{partId}/owner",
            description = "Part에 설정된 담당자(owner)와 담당팀(owner_team)을 조회합니다"
    )
    @GetMapping("/{partId}/owner")
    public PartOwnerResponse getPartOwner(
            @PathVariable UUID partId
    ) {
        return partOwnerQuery.getPartOwner(partId);
    }

    @Operation(
            summary = "PATCH /api/v1/parts/{partId}/owner",
            description = "포함된 필드만 부분 변경하며 null은 해제, 미포함 필드는 유지합니다"
    )
    @PatchMapping("/{partId}/owner")
    public PartOwnerResponse updatePartOwner(
            @PathVariable UUID partId,
            @Valid @RequestBody UpdatePartOwnerRequest request
    ) {
        UUID updatedPartId = updatePartOwnerUseCase.execute(partId, request);
        return partOwnerQuery.getPartOwner(updatedPartId);
    }

    @Operation(
            summary = "GET /api/v1/parts/owner/defaults",
            description = "카테고리별 기본 담당자/담당팀 설정 목록을 조회합니다 (category=null은 fallback)"
    )
    @GetMapping("/owner/defaults")
    public PartDefaultOwnerListResponse listDefaultOwners(
) {
        return partOwnerQuery.listDefaultOwners();
    }

    @Operation(
            summary = "PUT /api/v1/parts/owner/defaults",
            description = "카테고리 기본 담당자/담당팀 설정을 upsert합니다 (category=null은 fallback)"
    )
    @PutMapping("/owner/defaults")
    public PartDefaultOwnerItemResponse upsertDefaultOwner(
            @Valid @RequestBody PartDefaultOwnerRequest request
    ) {
        UUID defaultOwnerId = upsertDefaultOwnerUseCase.execute(request);
        return partOwnerQuery.getDefaultOwner(defaultOwnerId);
    }

    @Operation(
            summary = "DELETE /api/v1/parts/owner/defaults",
            description = "해당 카테고리의 기본 담당자/담당팀 설정을 삭제합니다 (미지정이면 fallback 삭제)"
    )
    @DeleteMapping("/owner/defaults")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDefaultOwner(
            @RequestParam(value = "category", required = false) String category
    ) {
        deleteDefaultOwnerUseCase.execute(category);
    }
}
