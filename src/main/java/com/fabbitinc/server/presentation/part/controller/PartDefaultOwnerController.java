package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartDefaultOwnerItemResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartDefaultOwnerListResponse;

import com.fabbitinc.server.application.part.dto.request.PartDefaultOwnerRequest;
import com.fabbitinc.server.application.part.dto.response.PartDefaultOwnerItemResponse;
import com.fabbitinc.server.application.part.dto.response.PartDefaultOwnerListResponse;
import com.fabbitinc.server.application.part.query.PartOwnerQuery;
import com.fabbitinc.server.application.part.query.condition.PartDefaultOwnerCondition;
import com.fabbitinc.server.application.part.usecase.DeleteDefaultOwnerUseCase;
import com.fabbitinc.server.application.part.usecase.UpsertDefaultOwnerUseCase;
import com.fabbitinc.server.application.part.usecase.command.DeleteDefaultOwnerCommand;
import com.fabbitinc.server.application.part.usecase.command.UpsertDefaultOwnerCommand;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts/owner/defaults")
@Tag(name = "part-default-owners", description = "부품 기본 담당자 설정 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartDefaultOwnerController {

    private final PartOwnerQuery partOwnerQuery;
    private final UpsertDefaultOwnerUseCase upsertDefaultOwnerUseCase;
    private final DeleteDefaultOwnerUseCase deleteDefaultOwnerUseCase;

    @Operation(summary = "GET /api/v1/parts/owner/defaults", description = "카테고리별 기본 담당자/담당팀 설정 목록을 조회합니다")
    @GetMapping
    public PartDefaultOwnerListResponse listDefaultOwners() {
        return toPartDefaultOwnerListResponse(partOwnerQuery.listDefaultOwners());
    }

    @Operation(summary = "PUT /api/v1/parts/owner/defaults", description = "카테고리 기본 담당자/담당팀 설정을 upsert합니다")
    @PutMapping
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

    @Operation(summary = "DELETE /api/v1/parts/owner/defaults", description = "해당 카테고리의 기본 담당자/담당팀 설정을 삭제합니다")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDefaultOwner(
            @Parameter(description = "삭제할 기본 담당자 카테고리", example = "MECHANICAL")
            @RequestParam(value = "category", required = false) String category
    ) {
        deleteDefaultOwnerUseCase.execute(new DeleteDefaultOwnerCommand(category));
    }
}
