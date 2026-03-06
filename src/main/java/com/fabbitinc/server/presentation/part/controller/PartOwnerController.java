package com.fabbitinc.server.presentation.part.controller;

import com.fabbitinc.server.application.part.dto.request.PartDefaultOwnerRequest;
import com.fabbitinc.server.application.part.dto.request.UpdatePartOwnerRequest;
import com.fabbitinc.server.application.part.dto.response.PartDefaultOwnerItemResponse;
import com.fabbitinc.server.application.part.dto.response.PartDefaultOwnerListResponse;
import com.fabbitinc.server.application.part.dto.response.PartOwnerResponse;
import com.fabbitinc.server.application.part.dto.response.PartOwnerUserSummaryResponse;
import com.fabbitinc.server.application.part.query.PartOwnerQuery;
import com.fabbitinc.server.application.part.query.condition.PartDefaultOwnerCondition;
import com.fabbitinc.server.application.part.query.condition.PartOwnerCondition;
import com.fabbitinc.server.application.part.query.result.PartDefaultOwnerListResult;
import com.fabbitinc.server.application.part.query.result.PartOwnerResult;
import com.fabbitinc.server.application.part.query.result.PartUserSummaryResult;
import com.fabbitinc.server.application.part.usecase.DeleteDefaultOwnerUseCase;
import com.fabbitinc.server.application.part.usecase.UpdatePartOwnerUseCase;
import com.fabbitinc.server.application.part.usecase.UpsertDefaultOwnerUseCase;
import com.fabbitinc.server.application.part.usecase.command.DeleteDefaultOwnerCommand;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartOwnerCommand;
import com.fabbitinc.server.application.part.usecase.command.UpsertDefaultOwnerCommand;
import com.fabbitinc.server.application.part.usecase.result.UpdatePartOwnerResult;
import com.fabbitinc.server.application.part.usecase.result.UpsertDefaultOwnerResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
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
            @Parameter(description = "담당자를 조회할 부품 ID")
            @PathVariable UUID partId
    ) {
        return toPartOwnerResponse(partOwnerQuery.get(new PartOwnerCondition(partId)));
    }

    @Operation(
            summary = "PATCH /api/v1/parts/{partId}/owner",
            description = "포함된 필드만 부분 변경하며 null은 해제, 미포함 필드는 유지합니다"
    )
    @PatchMapping("/{partId}/owner")
    public PartOwnerResponse updatePartOwner(
            @Parameter(description = "담당자를 수정할 부품 ID")
            @PathVariable UUID partId,
            @Parameter(description = "부품 담당자 수정 요청")
            @Valid @RequestBody UpdatePartOwnerRequest request
    ) {
        UpdatePartOwnerResult result = updatePartOwnerUseCase.execute(
                new UpdatePartOwnerCommand(
                        partId,
                        request.getOwnerId(),
                        request.isOwnerIdSet(),
                        request.getOwnerTeamId(),
                        request.isOwnerTeamIdSet()
                )
        );
        return toPartOwnerResponse(partOwnerQuery.get(new PartOwnerCondition(result.partId())));
    }

    @Operation(
            summary = "GET /api/v1/parts/owner/defaults",
            description = "카테고리별 기본 담당자/담당팀 설정 목록을 조회합니다 (category=null은 fallback)"
    )
    @GetMapping("/owner/defaults")
    public PartDefaultOwnerListResponse listDefaultOwners(
) {
        return toPartDefaultOwnerListResponse(partOwnerQuery.listDefaultOwners());
    }

    @Operation(
            summary = "PUT /api/v1/parts/owner/defaults",
            description = "카테고리 기본 담당자/담당팀 설정을 upsert합니다 (category=null은 fallback)"
    )
    @PutMapping("/owner/defaults")
    public PartDefaultOwnerItemResponse upsertDefaultOwner(
            @Parameter(description = "기본 담당자 upsert 요청")
            @Valid @RequestBody PartDefaultOwnerRequest request
    ) {
        UpsertDefaultOwnerResult result = upsertDefaultOwnerUseCase.execute(
                new UpsertDefaultOwnerCommand(
                        request.category(),
                        request.defaultOwnerId(),
                        request.defaultOwnerTeamId()
                )
        );
        return toPartDefaultOwnerItemResponse(partOwnerQuery.get(new PartDefaultOwnerCondition(result.defaultOwnerId())));
    }

    @Operation(
            summary = "DELETE /api/v1/parts/owner/defaults",
            description = "해당 카테고리의 기본 담당자/담당팀 설정을 삭제합니다 (미지정이면 fallback 삭제)"
    )
    @DeleteMapping("/owner/defaults")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDefaultOwner(
            @Parameter(description = "삭제할 기본 담당자 카테고리", example = "MECHANICAL")
            @RequestParam(value = "category", required = false) String category
    ) {
        deleteDefaultOwnerUseCase.execute(new DeleteDefaultOwnerCommand(category));
    }

    private PartOwnerResponse toPartOwnerResponse(PartOwnerResult result) {
        return new PartOwnerResponse(
                result.ownerId(),
                toPartOwnerUserSummaryResponse(result.owner()),
                result.ownerTeamId(),
                result.ownerTeamName()
        );
    }

    private PartDefaultOwnerListResponse toPartDefaultOwnerListResponse(PartDefaultOwnerListResult result) {
        return new PartDefaultOwnerListResponse(
                result.items().stream().map(this::toPartDefaultOwnerItemResponse).toList()
        );
    }

    private PartDefaultOwnerItemResponse toPartDefaultOwnerItemResponse(PartDefaultOwnerListResult.Item result) {
        return new PartDefaultOwnerItemResponse(
                result.id(),
                result.category(),
                result.defaultOwnerId(),
                toPartOwnerUserSummaryResponse(result.defaultOwner()),
                result.defaultOwnerTeamId(),
                result.defaultOwnerTeamName()
        );
    }

    private PartOwnerUserSummaryResponse toPartOwnerUserSummaryResponse(PartUserSummaryResult result) {
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
}
