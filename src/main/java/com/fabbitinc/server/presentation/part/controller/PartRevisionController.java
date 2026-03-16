package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toBomTreeResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartBomResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartDetailResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartFilesResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartOwnerResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartPreviewProcessingResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartPreviewResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartPreviewSourcesResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartProjectsResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartSuppliersResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toRegisterDrawingResponse;

import com.fabbitinc.server.presentation.common.web.ApiErrorResponse;
import com.fabbitinc.server.presentation.drawing.dto.request.RegisterDrawingRequest;
import com.fabbitinc.server.presentation.drawing.dto.response.RegisterDrawingResponse;
import com.fabbitinc.server.presentation.part.request.AttachFilesRequest;
import com.fabbitinc.server.presentation.part.request.ChangePartPreviewRequest;
import com.fabbitinc.server.presentation.part.request.CreatePartDraftRequest;
import com.fabbitinc.server.presentation.part.request.PartRevisionChangeReasonRequest;
import com.fabbitinc.server.presentation.part.request.UploadPartPreviewFileRequest;
import com.fabbitinc.server.presentation.part.request.UpdatePartOwnerRequest;
import com.fabbitinc.server.presentation.part.request.UpdatePartRevisionRequest;
import com.fabbitinc.server.presentation.part.response.BomTreeResponse;
import com.fabbitinc.server.presentation.part.response.PartAttachmentItemResponse;
import com.fabbitinc.server.presentation.part.response.PartBomResponse;
import com.fabbitinc.server.presentation.part.response.PartDetailResponse;
import com.fabbitinc.server.presentation.part.response.PartFilesResponse;
import com.fabbitinc.server.presentation.part.response.PartOwnerResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewProcessingResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewSourcesResponse;
import com.fabbitinc.server.presentation.part.response.PartProjectsResponse;
import com.fabbitinc.server.presentation.part.response.PartSuppliersResponse;
import com.fabbitinc.server.application.part.query.PartOwnerQuery;
import com.fabbitinc.server.application.part.query.PartPreviewProcessingQuery;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.BomTreeCondition;
import com.fabbitinc.server.application.part.query.condition.BomTreeExportCondition;
import com.fabbitinc.server.application.part.query.condition.FileItemsCondition;
import com.fabbitinc.server.application.part.query.condition.PartBomCondition;
import com.fabbitinc.server.application.part.query.condition.PartDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartDraftDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartFilesCondition;
import com.fabbitinc.server.application.part.query.condition.PartOwnerCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewSourcesCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewProcessingCondition;
import com.fabbitinc.server.application.part.query.condition.PartProjectsCondition;
import com.fabbitinc.server.application.part.query.condition.PartSuppliersCondition;
import com.fabbitinc.server.application.part.usecase.AttachPartFilesUseCase;
import com.fabbitinc.server.application.part.usecase.ApprovePartRevisionUseCase;
import com.fabbitinc.server.application.part.usecase.ChangePartPreviewUseCase;
import com.fabbitinc.server.application.part.usecase.ClearPartPreviewUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartPreviewFileUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.DetachPartFileUseCase;
import com.fabbitinc.server.application.part.usecase.RegisterPartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.ReleasePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.ReleasePartRevisionUseCase;
import com.fabbitinc.server.application.part.usecase.UploadPartPreviewFileUseCase;
import com.fabbitinc.server.application.part.usecase.UpdatePartOwnerUseCase;
import com.fabbitinc.server.application.part.usecase.UpdatePartRevisionUseCase;
import com.fabbitinc.server.application.part.usecase.command.ApprovePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.command.AttachPartFilesCommand;
import com.fabbitinc.server.application.part.usecase.command.ChangePartPreviewCommand;
import com.fabbitinc.server.application.part.usecase.command.ClearPartPreviewCommand;
import com.fabbitinc.server.application.part.usecase.command.CreatePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartPreviewFileCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.command.DetachPartFileCommand;
import com.fabbitinc.server.application.part.usecase.command.RegisterPartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.command.ReleasePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.ReleasePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.command.UploadPartPreviewFileCommand;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartOwnerCommand;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.result.ApprovePartRevisionResult;
import com.fabbitinc.server.application.part.usecase.result.AttachPartFilesResult;
import com.fabbitinc.server.application.part.usecase.result.CreatePartDraftResult;
import com.fabbitinc.server.application.part.usecase.result.ReleasePartDraftResult;
import com.fabbitinc.server.application.part.usecase.result.ReleasePartRevisionResult;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parts")
@Tag(name = "part-revisions", description = "부품 리비전/초안 관리 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartRevisionController {

    private static final String EXCEL_MEDIA_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final PartQuery partQuery;
    private final PartOwnerQuery partOwnerQuery;
    private final CreatePartDraftUseCase createPartDraftUseCase;
    private final ApprovePartRevisionUseCase approvePartRevisionUseCase;
    private final ReleasePartDraftUseCase releasePartDraftUseCase;
    private final ReleasePartRevisionUseCase releasePartRevisionUseCase;
    private final UpdatePartRevisionUseCase updatePartRevisionUseCase;
    private final UpdatePartOwnerUseCase updatePartOwnerUseCase;
    private final AttachPartFilesUseCase attachPartFilesUseCase;
    private final DetachPartFileUseCase detachPartFileUseCase;
    private final PartPreviewProcessingQuery partPreviewProcessingQuery;
    private final RegisterPartDrawingUseCase registerPartDrawingUseCase;
    private final DeletePartDrawingUseCase deletePartDrawingUseCase;
    private final ChangePartPreviewUseCase changePartPreviewUseCase;
    private final ClearPartPreviewUseCase clearPartPreviewUseCase;
    private final UploadPartPreviewFileUseCase uploadPartPreviewFileUseCase;
    private final DeletePartPreviewFileUseCase deletePartPreviewFileUseCase;

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/drafts", description = "기준 리비전에서 새 초안 리비전을 생성합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "409", description = "리소스 충돌"),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts")
    public PartDetailResponse createDraft(
            @Parameter(description = "기준 품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @RequestBody(required = false) CreatePartDraftRequest request
    ) {
        CreatePartDraftResult result = createPartDraftUseCase.execute(new CreatePartDraftCommand(
                partNumber,
                revisionCode,
                request == null ? null : request.reason()
        ));
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(
                result.partNumber(),
                revisionCode,
                result.draftKey()
        )));
    }

    @Operation(summary = "PATCH /api/v1/parts/{partNumber}/drafts/{draftKey}", description = "초기 DRAFT 상태의 부품 초안을 수정합니다")
    @PatchMapping("/{partNumber}/drafts/{draftKey}")
    public PartDetailResponse updateDraft(
            @PathVariable String partNumber,
            @PathVariable String draftKey,
            @Valid @RequestBody UpdatePartRevisionRequest request
    ) {
        updatePartRevisionUseCase.execute(new UpdatePartRevisionCommand(
                partNumber,
                null,
                draftKey,
                request.getName(),
                request.isNameSet(),
                request.getMaterial(),
                request.isMaterialSet(),
                request.getUnit(),
                request.isUnitSet(),
                request.getDescription(),
                request.isDescriptionSet(),
                request.getCategory(),
                request.isCategorySet(),
                request.getPhantom(),
                request.isPhantomSet(),
                request.getLeadTimeDays(),
                request.isLeadTimeDaysSet(),
                request.getExtendedProperties(),
                request.isExtendedPropertiesSet()
        ));
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, null, draftKey)));
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/drafts/{draftKey}", description = "초기 Part 초안 상세 정보와 관계 카운트를 조회합니다")
    @GetMapping("/{partNumber}/drafts/{draftKey}")
    public PartDetailResponse getDraft(
            @PathVariable String partNumber,
            @PathVariable String draftKey
    ) {
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, null, draftKey)));
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}", description = "특정 공식 리비전에서 파생된 Part 초안 상세를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}")
    public PartDetailResponse getRevisionDraft(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable String draftKey
    ) {
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, revisionCode, draftKey)));
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/drafts/{draftKey}/drawings", description = "초기 DRAFT 상태의 부품 초안에 도면을 업로드하고 등록합니다")
    @PostMapping("/{partNumber}/drafts/{draftKey}/drawings")
    public RegisterDrawingResponse createDraftDrawing(
            @PathVariable String partNumber,
            @PathVariable String draftKey,
            @Valid @RequestBody RegisterDrawingRequest request
    ) {
        return toRegisterDrawingResponse(registerPartDrawingUseCase.execute(
                new RegisterPartDrawingCommand(partNumber, null, draftKey, request.fileId())
        ));
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/drawings", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 초안에 도면을 업로드하고 등록합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/drawings")
    public RegisterDrawingResponse createRevisionDraftDrawing(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable String draftKey,
            @Valid @RequestBody RegisterDrawingRequest request
    ) {
        return toRegisterDrawingResponse(registerPartDrawingUseCase.execute(
                new RegisterPartDrawingCommand(partNumber, revisionCode, draftKey, request.fileId())
        ));
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/drafts/{draftKey}/approve", description = "초기 Part 초안을 직접 승인하고 공식 리비전으로 전환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "승인 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "현재 워크플로 정책상 직접 승인 불가 또는 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "workflow_policy_forbidden",
                                    value = "{\"code\":\"PART_WORKFLOW_POLICY_FORBIDDEN\",\"message\":\"변경관리 모드에서는 직접 승인/릴리즈를 사용할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 승인됨, 최신 초안 아님, 현재 상태에서 승인 불가 등 리소스 상태 충돌",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "invalid_state",
                                    value = "{\"code\":\"INVALID_STATE\",\"message\":\"현재 상태에서는 승인할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/drafts/{draftKey}/approve")
    public PartDetailResponse approveDraft(
            @PathVariable String partNumber,
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ApprovePartRevisionResult result = approvePartRevisionUseCase.execute(new ApprovePartRevisionCommand(
                partNumber,
                null,
                draftKey,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(result.partNumber(), result.revisionCode())));
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/drafts/{draftKey}/release", description = "초기 Part 초안을 직접 릴리즈하고 공식 리비전으로 전환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "릴리즈 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "현재 워크플로 정책상 직접 릴리즈 불가 또는 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "workflow_policy_forbidden",
                                    value = "{\"code\":\"PART_WORKFLOW_POLICY_FORBIDDEN\",\"message\":\"변경관리 모드에서는 직접 승인/릴리즈를 사용할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 릴리즈됨, 최신 초안 아님, 현재 상태에서 릴리즈 불가 등 리소스 상태 충돌",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "invalid_state",
                                    value = "{\"code\":\"INVALID_STATE\",\"message\":\"현재 상태에서는 릴리즈할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/drafts/{draftKey}/release")
    public PartDetailResponse releaseDraft(
            @PathVariable String partNumber,
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ReleasePartDraftResult result = releasePartDraftUseCase.execute(new ReleasePartDraftCommand(
                partNumber,
                null,
                draftKey,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(result.partNumber(), result.revisionCode())));
    }

    @Operation(summary = "PATCH /api/v1/parts/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안을 수정합니다")
    @PatchMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}")
    public PartDetailResponse updateRevisionDraft(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable String draftKey,
            @Valid @RequestBody UpdatePartRevisionRequest request
    ) {
        updatePartRevisionUseCase.execute(new UpdatePartRevisionCommand(
                partNumber,
                revisionCode,
                draftKey,
                request.getName(),
                request.isNameSet(),
                request.getMaterial(),
                request.isMaterialSet(),
                request.getUnit(),
                request.isUnitSet(),
                request.getDescription(),
                request.isDescriptionSet(),
                request.getCategory(),
                request.isCategorySet(),
                request.getPhantom(),
                request.isPhantomSet(),
                request.getLeadTimeDays(),
                request.isLeadTimeDaysSet(),
                request.getExtendedProperties(),
                request.isExtendedPropertiesSet()
        ));
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, revisionCode, draftKey)));
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/approve", description = "특정 공식 리비전에서 파생된 초안을 직접 승인하고 새 공식 리비전으로 전환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "승인 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "현재 워크플로 정책상 직접 승인 불가 또는 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "workflow_policy_forbidden",
                                    value = "{\"code\":\"PART_WORKFLOW_POLICY_FORBIDDEN\",\"message\":\"변경관리 모드에서는 직접 승인/릴리즈를 사용할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 승인됨, 최신 초안 아님, 현재 상태에서 승인 불가 등 리소스 상태 충돌",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "conflict",
                                    value = "{\"code\":\"CONFLICT\",\"message\":\"최신 공식 리비전을 기준으로 다시 초안을 만들어야 합니다\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/approve")
    public PartDetailResponse approveRevisionDraft(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ApprovePartRevisionResult result = approvePartRevisionUseCase.execute(new ApprovePartRevisionCommand(
                partNumber,
                revisionCode,
                draftKey,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(result.partNumber(), result.revisionCode())));
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/release", description = "특정 공식 리비전에서 파생된 초안을 직접 릴리즈하고 새 공식 리비전으로 전환합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "릴리즈 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "현재 워크플로 정책상 직접 릴리즈 불가 또는 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "workflow_policy_forbidden",
                                    value = "{\"code\":\"PART_WORKFLOW_POLICY_FORBIDDEN\",\"message\":\"변경관리 모드에서는 직접 승인/릴리즈를 사용할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 릴리즈됨, 최신 초안 아님, 현재 상태에서 릴리즈 불가 등 리소스 상태 충돌",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "conflict",
                                    value = "{\"code\":\"CONFLICT\",\"message\":\"최신 공식 리비전을 기준으로 다시 초안을 만들어야 합니다\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/release")
    public PartDetailResponse releaseRevisionDraft(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ReleasePartDraftResult result = releasePartDraftUseCase.execute(new ReleasePartDraftCommand(
                partNumber,
                revisionCode,
                draftKey,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(result.partNumber(), result.revisionCode())));
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}", description = "Part 상세 정보와 관계 카운트를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}")
    public PartDetailResponse getPart(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/release", description = "승인된 공식 리비전을 직접 릴리즈합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "릴리즈 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "현재 워크플로 정책상 직접 릴리즈 불가 또는 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "workflow_policy_forbidden",
                                    value = "{\"code\":\"PART_WORKFLOW_POLICY_FORBIDDEN\",\"message\":\"변경관리 모드에서는 직접 승인/릴리즈를 사용할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 릴리즈됨, 최신 승인 리비전 아님, 현재 상태에서 릴리즈 불가 등 리소스 상태 충돌",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "invalid_state",
                                    value = "{\"code\":\"INVALID_STATE\",\"message\":\"현재 최신 승인 리비전이 아닙니다. 최신 승인 리비전만 릴리즈할 수 있습니다\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/release")
    public PartDetailResponse releaseRevision(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ReleasePartRevisionResult result = releasePartRevisionUseCase.execute(new ReleasePartRevisionCommand(
                partNumber,
                revisionCode,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new PartDetailCondition(result.partNumber(), result.revisionCode())));
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/owner", description = "Part에 설정된 담당자와 담당팀을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/owner")
    public PartOwnerResponse getPartOwner(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartOwnerResponse(partOwnerQuery.get(new PartOwnerCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "PATCH /api/v1/parts/{partNumber}/revisions/{revisionCode}/owner", description = "포함된 필드만 부분 변경하며 null은 해제, 미포함 필드는 유지합니다")
    @PatchMapping("/{partNumber}/revisions/{revisionCode}/owner")
    public PartOwnerResponse updatePartOwner(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @Valid @RequestBody UpdatePartOwnerRequest request
    ) {
        updatePartOwnerUseCase.execute(
                new UpdatePartOwnerCommand(
                        partNumber,
                        revisionCode,
                        request.getOwnerId(),
                        request.isOwnerIdSet(),
                        request.getOwnerTeamId(),
                        request.isOwnerTeamIdSet()
                )
        );
        return toPartOwnerResponse(partOwnerQuery.get(new PartOwnerCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/bom", description = "Part의 직접 자식/직접 부모 BOM 관계를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/bom")
    public PartBomResponse getPartBom(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartBomResponse(partQuery.get(new PartBomCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/bom/tree", description = "Part BOM 트리를 정전개 또는 역전개로 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/bom/tree")
    public BomTreeResponse getBomTree(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction
    ) {
        return toBomTreeResponse(partQuery.getBomTree(new BomTreeCondition(partNumber, revisionCode, direction)));
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/bom/tree/export", description = "Part BOM 트리를 Excel(.xlsx) 파일로 내보냅니다")
    @GetMapping(value = "/{partNumber}/revisions/{revisionCode}/bom/tree/export", produces = EXCEL_MEDIA_TYPE)
    public ResponseEntity<byte[]> exportBomTree(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @RequestParam(value = "direction", defaultValue = "FORWARD") String direction,
            @RequestParam(value = "mapping_id", required = false) UUID mappingId
    ) {
        byte[] content = partQuery.exportBomTree(new BomTreeExportCondition(
                partNumber,
                revisionCode,
                direction,
                mappingId
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BOM.xlsx");
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/projects", description = "해당 Part가 소속된 프로젝트 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/projects")
    public PartProjectsResponse getPartProjects(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartProjectsResponse(partQuery.get(new PartProjectsCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/files", description = "Part에 연결된 업로드 완료 파일 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/files")
    public PartFilesResponse getPartFiles(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartFilesResponse(partQuery.get(new PartFilesCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/suppliers", description = "Part에 연결된 공급사 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/suppliers")
    public PartSuppliersResponse getPartSuppliers(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartSuppliersResponse(partQuery.get(new PartSuppliersCondition(partNumber, revisionCode)));
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/files", description = "업로드 완료 파일들을 Part에 배치 연결합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/files")
    public List<PartAttachmentItemResponse> attachFiles(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        AttachPartFilesResult result = attachPartFilesUseCase.execute(
                new AttachPartFilesCommand(partNumber, revisionCode, request.fileIds())
        );
        return partQuery.getFiles(new FileItemsCondition(result.fileIds())).stream()
                .map(PartResponseMapper::toPartAttachmentItemResponse)
                .toList();
    }

    @Operation(summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/files/{fileId}", description = "Part에 연결된 첨부파일 1건을 제거합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/files/{fileId}")
    public ResponseEntity<Void> detachFile(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable UUID fileId
    ) {
        detachPartFileUseCase.execute(new DetachPartFileCommand(partNumber, revisionCode, fileId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/drawings/{drawingId}", description = "PartRevision에 연결된 도면 1건을 삭제합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/drawings/{drawingId}")
    public ResponseEntity<Void> deleteDrawingFromPart(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable UUID drawingId
    ) {
        deletePartDrawingUseCase.execute(new DeletePartDrawingCommand(partNumber, revisionCode, drawingId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/drawings", description = "업로드 완료 파일을 Drawing으로 등록하고 PartRevision에 연결합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drawings")
    public RegisterDrawingResponse registerDrawingForPart(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @Valid @RequestBody RegisterDrawingRequest request
    ) {
        return toRegisterDrawingResponse(registerPartDrawingUseCase.execute(
                new RegisterPartDrawingCommand(partNumber, revisionCode, null, request.fileId())
        ));
    }

    @Operation(summary = "PATCH /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview", description = "Part 대표 미리보기 소스를 도면 또는 미리보기 전용 파일로 변경합니다")
    @PatchMapping("/{partNumber}/revisions/{revisionCode}/preview")
    public PartPreviewResponse updatePreview(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @Valid @RequestBody ChangePartPreviewRequest request
    ) {
        changePartPreviewUseCase.execute(new ChangePartPreviewCommand(
                partNumber,
                revisionCode,
                request.sourceType(),
                request.sourceId()
        ));
        return toPartPreviewResponse(partQuery.get(new PartDetailCondition(partNumber, revisionCode)).preview());
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview/sources", description = "대표 미리보기 선택 모달에 필요한 선택 가능 소스 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/preview/sources")
    public PartPreviewSourcesResponse getPreviewSources(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartPreviewSourcesResponse(
                partQuery.getPreviewSources(new PartPreviewSourcesCondition(partNumber, revisionCode))
        );
    }

    @Operation(summary = "POST /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview/files", description = "업로드 완료 파일을 대표 미리보기 전용 파일로 등록하고 현재 미리보기로 설정합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/preview/files")
    public PartPreviewResponse uploadPreviewFile(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @Valid @RequestBody UploadPartPreviewFileRequest request
    ) {
        uploadPartPreviewFileUseCase.execute(new UploadPartPreviewFileCommand(
                partNumber,
                revisionCode,
                request.fileId()
        ));
        return toPartPreviewResponse(partQuery.get(new PartDetailCondition(partNumber, revisionCode)).preview());
    }

    @Operation(summary = "GET /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview/processing", description = "Part 대표 미리보기 비동기 처리 상태와 산출물 준비 여부를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/preview/processing")
    public PartPreviewProcessingResponse getPreviewProcessing(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        return toPartPreviewProcessingResponse(
                partPreviewProcessingQuery.get(new PartPreviewProcessingCondition(partNumber, revisionCode))
        );
    }

    @Operation(summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview", description = "Part 대표 미리보기를 해제합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/preview")
    public ResponseEntity<Void> deletePreview(
            @PathVariable String partNumber,
            @PathVariable String revisionCode
    ) {
        clearPartPreviewUseCase.execute(new ClearPartPreviewCommand(partNumber, revisionCode));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "DELETE /api/v1/parts/{partNumber}/revisions/{revisionCode}/preview/files/{previewFileId}", description = "대표 미리보기 전용 파일 1건을 삭제합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/preview/files/{previewFileId}")
    public ResponseEntity<Void> deletePreviewFile(
            @PathVariable String partNumber,
            @PathVariable String revisionCode,
            @PathVariable UUID previewFileId
    ) {
        deletePartPreviewFileUseCase.execute(new DeletePartPreviewFileCommand(partNumber, revisionCode, previewFileId));
        return ResponseEntity.noContent().build();
    }
}
