package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartDetailResponse;

import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.usecase.CancelPartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.ReleasePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.UpdatePartRevisionUseCase;
import com.fabbitinc.server.application.part.usecase.command.CancelPartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.CreatePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.ReleasePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.result.CreatePartDraftResult;
import com.fabbitinc.server.application.part.usecase.result.ReleasePartDraftResult;
import com.fabbitinc.server.presentation.part.request.CreatePartDraftRequest;
import com.fabbitinc.server.presentation.part.request.PartRevisionChangeReasonRequest;
import com.fabbitinc.server.presentation.part.request.UpdatePartRevisionRequest;
import com.fabbitinc.server.presentation.part.response.PartDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "part-revision-commands", description = "부품 리비전 생성/수정/상태 전이 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartRevisionCommandController {

    private final PartQuery partQuery;
    private final CreatePartDraftUseCase createPartDraftUseCase;
    private final UpdatePartRevisionUseCase updatePartRevisionUseCase;
    private final ReleasePartDraftUseCase releasePartDraftUseCase;
    private final CancelPartDraftUseCase cancelPartDraftUseCase;

    @Operation(summary = "기준 리비전에서 새 DRAFT 리비전을 생성합니다", description = "기준 리비전에서 새 DRAFT 리비전을 생성합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "409", description = "리소스 충돌")
    })
    @PostMapping("/{partId}/revisions/{revisionId}/draft")
    public PartDetailResponse createDraft(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @RequestBody(required = false) CreatePartDraftRequest request
    ) {
        CreatePartDraftResult result = createPartDraftUseCase.execute(
                new CreatePartDraftCommand(partId, revisionId, request == null ? null : request.reason())
        );
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(result.partId(), result.revisionId())));
    }

    @Operation(summary = "리비전을 수정합니다", description = "DRAFT 상태의 리비전을 수정합니다")
    @PatchMapping("/{partId}/revisions/{revisionId}")
    public PartDetailResponse update(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody UpdatePartRevisionRequest request
    ) {
        updatePartRevisionUseCase.execute(new UpdatePartRevisionCommand(
                partId,
                revisionId,
                request.getName(),
                request.isNameSet(),
                request.getMaterial(),
                request.isMaterialSet(),
                request.getUnit(),
                request.isUnitSet(),
                request.getDescription(),
                request.isDescriptionSet(),
                request.getLeadTimeDays(),
                request.isLeadTimeDaysSet(),
                request.getExtendedProperties(),
                request.isExtendedPropertiesSet()
        ));
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(partId, revisionId)));
    }

    @Operation(summary = "리비전을 직접 반영합니다", description = "DRAFT 상태 리비전을 직접 반영해 공식 리비전으로 전환합니다")
    @PostMapping("/{partId}/revisions/{revisionId}/release")
    public PartDetailResponse release(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ReleasePartDraftResult result = releasePartDraftUseCase.execute(
                new ReleasePartDraftCommand(partId, revisionId, request.reason())
        );
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(result.partId(), result.revisionId())));
    }

    @Operation(summary = "리비전을 폐기합니다", description = "DRAFT 상태 리비전을 폐기합니다")
    @PostMapping("/{partId}/revisions/{revisionId}/cancel")
    public PartDetailResponse cancel(
            @PathVariable UUID partId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        cancelPartDraftUseCase.execute(new CancelPartDraftCommand(partId, revisionId, request.reason()));
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(partId, revisionId)));
    }
}
