package com.fabbitinc.server.presentation.part.controller;

import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartDetailResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartFilesResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartPreviewProcessingResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartPreviewResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toPartPreviewSourcesResponse;
import static com.fabbitinc.server.presentation.part.controller.PartResponseMapper.toRegisterDrawingResponse;

import com.fabbitinc.server.application.part.query.PartPreviewProcessingQuery;
import com.fabbitinc.server.application.part.query.PartQuery;
import com.fabbitinc.server.application.part.query.condition.FileItemsCondition;
import com.fabbitinc.server.application.part.query.condition.PartDraftDetailCondition;
import com.fabbitinc.server.application.part.query.condition.PartFilesCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewProcessingCondition;
import com.fabbitinc.server.application.part.query.condition.PartPreviewSourcesCondition;
import com.fabbitinc.server.application.part.usecase.ApprovePartRevisionUseCase;
import com.fabbitinc.server.application.part.usecase.AttachPartFilesUseCase;
import com.fabbitinc.server.application.part.usecase.CancelPartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.ChangePartPreviewUseCase;
import com.fabbitinc.server.application.part.usecase.ClearPartPreviewUseCase;
import com.fabbitinc.server.application.part.usecase.CreatePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.DeletePartPreviewFileUseCase;
import com.fabbitinc.server.application.part.usecase.DetachPartFileUseCase;
import com.fabbitinc.server.application.part.usecase.RegisterPartDrawingUseCase;
import com.fabbitinc.server.application.part.usecase.ReleasePartDraftUseCase;
import com.fabbitinc.server.application.part.usecase.UploadPartPreviewFileUseCase;
import com.fabbitinc.server.application.part.usecase.UpdatePartRevisionUseCase;
import com.fabbitinc.server.application.part.usecase.command.ApprovePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.command.AttachPartFilesCommand;
import com.fabbitinc.server.application.part.usecase.command.CancelPartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.ChangePartPreviewCommand;
import com.fabbitinc.server.application.part.usecase.command.ClearPartPreviewCommand;
import com.fabbitinc.server.application.part.usecase.command.CreatePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.command.DeletePartPreviewFileCommand;
import com.fabbitinc.server.application.part.usecase.command.DetachPartFileCommand;
import com.fabbitinc.server.application.part.usecase.command.RegisterPartDrawingCommand;
import com.fabbitinc.server.application.part.usecase.command.ReleasePartDraftCommand;
import com.fabbitinc.server.application.part.usecase.command.UpdatePartRevisionCommand;
import com.fabbitinc.server.application.part.usecase.command.UploadPartPreviewFileCommand;
import com.fabbitinc.server.application.part.usecase.result.ApprovePartRevisionResult;
import com.fabbitinc.server.application.part.usecase.result.AttachPartFilesResult;
import com.fabbitinc.server.application.part.usecase.result.CreatePartDraftResult;
import com.fabbitinc.server.application.part.usecase.result.ReleasePartDraftResult;
import com.fabbitinc.server.presentation.common.web.ApiErrorResponse;
import com.fabbitinc.server.presentation.drawing.dto.request.RegisterDrawingRequest;
import com.fabbitinc.server.presentation.drawing.dto.response.RegisterDrawingResponse;
import com.fabbitinc.server.presentation.part.request.AttachFilesRequest;
import com.fabbitinc.server.presentation.part.request.ChangePartPreviewRequest;
import com.fabbitinc.server.presentation.part.request.CreatePartDraftRequest;
import com.fabbitinc.server.presentation.part.request.PartRevisionChangeReasonRequest;
import com.fabbitinc.server.presentation.part.request.UpdatePartRevisionRequest;
import com.fabbitinc.server.presentation.part.request.UploadPartPreviewFileRequest;
import com.fabbitinc.server.presentation.part.response.PartAttachmentItemResponse;
import com.fabbitinc.server.presentation.part.response.PartDetailResponse;
import com.fabbitinc.server.presentation.part.response.PartFilesResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewProcessingResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewResponse;
import com.fabbitinc.server.presentation.part.response.PartPreviewSourcesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@Tag(name = "part-drafts", description = "부품 초안 생성, 조회, 수정 및 자산 관리 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class PartDraftController {

    private final PartQuery partQuery;
    private final PartPreviewProcessingQuery partPreviewProcessingQuery;
    private final CreatePartDraftUseCase createPartDraftUseCase;
    private final UpdatePartRevisionUseCase updatePartRevisionUseCase;
    private final ApprovePartRevisionUseCase approvePartRevisionUseCase;
    private final ReleasePartDraftUseCase releasePartDraftUseCase;
    private final CancelPartDraftUseCase cancelPartDraftUseCase;
    private final AttachPartFilesUseCase attachPartFilesUseCase;
    private final DetachPartFileUseCase detachPartFileUseCase;
    private final RegisterPartDrawingUseCase registerPartDrawingUseCase;
    private final DeletePartDrawingUseCase deletePartDrawingUseCase;
    private final ChangePartPreviewUseCase changePartPreviewUseCase;
    private final ClearPartPreviewUseCase clearPartPreviewUseCase;
    private final UploadPartPreviewFileUseCase uploadPartPreviewFileUseCase;
    private final DeletePartPreviewFileUseCase deletePartPreviewFileUseCase;

    @Operation(summary = "기준 리비전에서 새 초안 리비전을 생성합니다", description = "기준 리비전에서 새 초안 리비전을 생성합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공"),
            @ApiResponse(responseCode = "409", description = "리소스 충돌"),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts")
    public PartDetailResponse create(
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

    @Operation(summary = "초기 DRAFT 상태의 부품 초안을 수정합니다", description = "초기 DRAFT 상태의 부품 초안을 수정합니다")
    @PatchMapping("/{partNumber}/drafts/{draftKey}")
    public PartDetailResponse update(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
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

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안을 수정합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안을 수정합니다")
    @PatchMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}")
    public PartDetailResponse updateFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
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

    @Operation(summary = "초기 Part 초안 상세 정보와 관계 카운트를 조회합니다", description = "초기 Part 초안 상세 정보와 관계 카운트를 조회합니다")
    @GetMapping("/{partNumber}/drafts/{draftKey}")
    public PartDetailResponse get(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, null, draftKey)));
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 Part 초안 상세를 조회합니다", description = "특정 공식 리비전에서 파생된 Part 초안 상세를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}")
    public PartDetailResponse getFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        return toPartDetailResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, revisionCode, draftKey)));
    }

    @Operation(summary = "초기 Part 초안을 직접 승인하고 공식 리비전으로 전환합니다", description = "초기 Part 초안을 직접 승인하고 공식 리비전으로 전환합니다")
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
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/drafts/{draftKey}/approve")
    public PartDetailResponse approve(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ApprovePartRevisionResult result = approvePartRevisionUseCase.execute(new ApprovePartRevisionCommand(
                partNumber,
                null,
                draftKey,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new com.fabbitinc.server.application.part.query.condition.PartDetailCondition(
                result.partNumber(),
                result.revisionCode()
        )));
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 초안을 직접 승인하고 새 공식 리비전으로 전환합니다", description = "특정 공식 리비전에서 파생된 초안을 직접 승인하고 새 공식 리비전으로 전환합니다")
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
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/approve")
    public PartDetailResponse approveFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ApprovePartRevisionResult result = approvePartRevisionUseCase.execute(new ApprovePartRevisionCommand(
                partNumber,
                revisionCode,
                draftKey,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new com.fabbitinc.server.application.part.query.condition.PartDetailCondition(
                result.partNumber(),
                result.revisionCode()
        )));
    }

    @Operation(summary = "초기 Part 초안을 직접 릴리즈하고 공식 리비전으로 전환합니다", description = "초기 Part 초안을 직접 릴리즈하고 공식 리비전으로 전환합니다")
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
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/drafts/{draftKey}/release")
    public PartDetailResponse release(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ReleasePartDraftResult result = releasePartDraftUseCase.execute(new ReleasePartDraftCommand(
                partNumber,
                null,
                draftKey,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new com.fabbitinc.server.application.part.query.condition.PartDetailCondition(
                result.partNumber(),
                result.revisionCode()
        )));
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 초안을 직접 릴리즈하고 새 공식 리비전으로 전환합니다", description = "특정 공식 리비전에서 파생된 초안을 직접 릴리즈하고 새 공식 리비전으로 전환합니다")
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
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/release")
    public PartDetailResponse releaseFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        ReleasePartDraftResult result = releasePartDraftUseCase.execute(new ReleasePartDraftCommand(
                partNumber,
                revisionCode,
                draftKey,
                request.reason()
        ));
        return toPartDetailResponse(partQuery.get(new com.fabbitinc.server.application.part.query.condition.PartDetailCondition(
                result.partNumber(),
                result.revisionCode()
        )));
    }

    @Operation(summary = "초기 Part 초안을 직접 취소합니다", description = "초기 Part 초안을 직접 취소합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "현재 워크플로 정책상 직접 취소 불가 또는 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "workflow_policy_forbidden",
                                    value = "{\"code\":\"PART_WORKFLOW_POLICY_FORBIDDEN\",\"message\":\"변경관리 모드에서는 직접 승인/릴리즈를 사용할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/drafts/{draftKey}/cancel")
    public ResponseEntity<Void> cancel(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        cancelPartDraftUseCase.execute(new CancelPartDraftCommand(partNumber, null, draftKey, request.reason()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 초안을 직접 취소합니다", description = "특정 공식 리비전에서 파생된 초안을 직접 취소합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "취소 성공"),
            @ApiResponse(
                    responseCode = "403",
                    description = "현재 워크플로 정책상 직접 취소 불가 또는 권한 없음",
                    content = @Content(
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "workflow_policy_forbidden",
                                    value = "{\"code\":\"PART_WORKFLOW_POLICY_FORBIDDEN\",\"message\":\"변경관리 모드에서는 직접 승인/릴리즈를 사용할 수 없습니다\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "422", description = "입력값 검증 실패")
    })
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/cancel")
    public ResponseEntity<Void> cancelFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody PartRevisionChangeReasonRequest request
    ) {
        cancelPartDraftUseCase.execute(new CancelPartDraftCommand(partNumber, revisionCode, draftKey, request.reason()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "초기 DRAFT 상태의 부품 초안에 연결된 업로드 완료 파일 목록을 조회합니다", description = "초기 DRAFT 상태의 부품 초안에 연결된 업로드 완료 파일 목록을 조회합니다")
    @GetMapping("/{partNumber}/drafts/{draftKey}/files")
    public PartFilesResponse getFiles(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        return toPartFilesResponse(partQuery.get(new PartFilesCondition(partNumber, null, null, draftKey)));
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안에 연결된 업로드 완료 파일 목록을 조회합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안에 연결된 업로드 완료 파일 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/files")
    public PartFilesResponse getFilesFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        return toPartFilesResponse(partQuery.get(new PartFilesCondition(partNumber, null, revisionCode, draftKey)));
    }

    @Operation(summary = "업로드 완료 파일들을 초기 DRAFT 상태의 부품 초안에 배치 연결합니다", description = "업로드 완료 파일들을 초기 DRAFT 상태의 부품 초안에 배치 연결합니다")
    @PostMapping("/{partNumber}/drafts/{draftKey}/files")
    public List<PartAttachmentItemResponse> attachFiles(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        AttachPartFilesResult result = attachPartFilesUseCase.execute(
                new AttachPartFilesCommand(partNumber, null, null, draftKey, request.fileIds())
        );
        return partQuery.getFiles(new FileItemsCondition(result.fileIds())).stream()
                .map(PartResponseMapper::toPartAttachmentItemResponse)
                .toList();
    }

    @Operation(summary = "업로드 완료 파일들을 특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안에 배치 연결합니다", description = "업로드 완료 파일들을 특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안에 배치 연결합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/files")
    public List<PartAttachmentItemResponse> attachFilesFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        AttachPartFilesResult result = attachPartFilesUseCase.execute(
                new AttachPartFilesCommand(partNumber, null, revisionCode, draftKey, request.fileIds())
        );
        return partQuery.getFiles(new FileItemsCondition(result.fileIds())).stream()
                .map(PartResponseMapper::toPartAttachmentItemResponse)
                .toList();
    }

    @Operation(summary = "초기 DRAFT 상태의 부품 초안에 연결된 첨부파일 1건을 제거합니다", description = "초기 DRAFT 상태의 부품 초안에 연결된 첨부파일 1건을 제거합니다")
    @DeleteMapping("/{partNumber}/drafts/{draftKey}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Parameter(description = "파일 ID")
            @PathVariable UUID fileId
    ) {
        detachPartFileUseCase.execute(new DetachPartFileCommand(partNumber, null, null, draftKey, fileId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안에 연결된 첨부파일 1건을 제거합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안에 연결된 첨부파일 1건을 제거합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/files/{fileId}")
    public ResponseEntity<Void> deleteFileFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Parameter(description = "파일 ID")
            @PathVariable UUID fileId
    ) {
        detachPartFileUseCase.execute(new DetachPartFileCommand(partNumber, null, revisionCode, draftKey, fileId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "초기 DRAFT 상태의 부품 초안에 도면을 업로드하고 등록합니다", description = "초기 DRAFT 상태의 부품 초안에 도면을 업로드하고 등록합니다")
    @PostMapping("/{partNumber}/drafts/{draftKey}/drawings")
    public RegisterDrawingResponse createDrawing(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody RegisterDrawingRequest request
    ) {
        return toRegisterDrawingResponse(registerPartDrawingUseCase.execute(
                new RegisterPartDrawingCommand(partNumber, null, draftKey, request.fileId())
        ));
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 초안에 도면을 업로드하고 등록합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 초안에 도면을 업로드하고 등록합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/drawings")
    public RegisterDrawingResponse createDrawingFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody RegisterDrawingRequest request
    ) {
        return toRegisterDrawingResponse(registerPartDrawingUseCase.execute(
                new RegisterPartDrawingCommand(partNumber, revisionCode, draftKey, request.fileId())
        ));
    }

    @Operation(summary = "초기 DRAFT 상태의 부품 초안에 연결된 도면 1건을 삭제합니다", description = "초기 DRAFT 상태의 부품 초안에 연결된 도면 1건을 삭제합니다")
    @DeleteMapping("/{partNumber}/drafts/{draftKey}/drawings/{drawingId}")
    public ResponseEntity<Void> deleteDrawing(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Parameter(description = "도면 ID")
            @PathVariable UUID drawingId
    ) {
        deletePartDrawingUseCase.execute(new DeletePartDrawingCommand(partNumber, null, null, draftKey, drawingId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안에 연결된 도면 1건을 삭제합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안에 연결된 도면 1건을 삭제합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/drawings/{drawingId}")
    public ResponseEntity<Void> deleteDrawingFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Parameter(description = "도면 ID")
            @PathVariable UUID drawingId
    ) {
        deletePartDrawingUseCase.execute(new DeletePartDrawingCommand(partNumber, null, revisionCode, draftKey, drawingId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "초기 DRAFT 상태의 부품 초안 대표 미리보기 소스를 도면 또는 미리보기 전용 파일로 변경합니다", description = "초기 DRAFT 상태의 부품 초안 대표 미리보기 소스를 도면 또는 미리보기 전용 파일로 변경합니다")
    @PatchMapping("/{partNumber}/drafts/{draftKey}/preview")
    public PartPreviewResponse updatePreview(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody ChangePartPreviewRequest request
    ) {
        changePartPreviewUseCase.execute(new ChangePartPreviewCommand(
                partNumber,
                null,
                null,
                draftKey,
                request.sourceType(),
                request.sourceId()
        ));
        return toPartPreviewResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, null, draftKey)).preview());
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 소스를 도면 또는 미리보기 전용 파일로 변경합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 소스를 도면 또는 미리보기 전용 파일로 변경합니다")
    @PatchMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/preview")
    public PartPreviewResponse updatePreviewFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody ChangePartPreviewRequest request
    ) {
        changePartPreviewUseCase.execute(new ChangePartPreviewCommand(
                partNumber,
                null,
                revisionCode,
                draftKey,
                request.sourceType(),
                request.sourceId()
        ));
        return toPartPreviewResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, revisionCode, draftKey)).preview());
    }

    @Operation(summary = "초기 DRAFT 상태의 부품 초안 대표 미리보기 선택 모달에 필요한 선택 가능 소스 목록을 조회합니다", description = "초기 DRAFT 상태의 부품 초안 대표 미리보기 선택 모달에 필요한 선택 가능 소스 목록을 조회합니다")
    @GetMapping("/{partNumber}/drafts/{draftKey}/preview/sources")
    public PartPreviewSourcesResponse getPreviewSources(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        return toPartPreviewSourcesResponse(
                partQuery.getPreviewSources(new PartPreviewSourcesCondition(partNumber, null, null, draftKey))
        );
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 선택 모달에 필요한 선택 가능 소스 목록을 조회합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 선택 모달에 필요한 선택 가능 소스 목록을 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/preview/sources")
    public PartPreviewSourcesResponse getPreviewSourcesFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        return toPartPreviewSourcesResponse(
                partQuery.getPreviewSources(new PartPreviewSourcesCondition(partNumber, null, revisionCode, draftKey))
        );
    }

    @Operation(summary = "업로드 완료 파일을 초기 DRAFT 상태의 부품 초안 대표 미리보기 전용 파일로 등록하고 현재 미리보기로 설정합니다", description = "업로드 완료 파일을 초기 DRAFT 상태의 부품 초안 대표 미리보기 전용 파일로 등록하고 현재 미리보기로 설정합니다")
    @PostMapping("/{partNumber}/drafts/{draftKey}/preview/files")
    public PartPreviewResponse uploadPreviewFile(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody UploadPartPreviewFileRequest request
    ) {
        uploadPartPreviewFileUseCase.execute(new UploadPartPreviewFileCommand(
                partNumber,
                null,
                null,
                draftKey,
                request.fileId()
        ));
        return toPartPreviewResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, null, draftKey)).preview());
    }

    @Operation(summary = "업로드 완료 파일을 특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 전용 파일로 등록하고 현재 미리보기로 설정합니다", description = "업로드 완료 파일을 특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 전용 파일로 등록하고 현재 미리보기로 설정합니다")
    @PostMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/preview/files")
    public PartPreviewResponse uploadPreviewFileFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Valid @RequestBody UploadPartPreviewFileRequest request
    ) {
        uploadPartPreviewFileUseCase.execute(new UploadPartPreviewFileCommand(
                partNumber,
                null,
                revisionCode,
                draftKey,
                request.fileId()
        ));
        return toPartPreviewResponse(partQuery.getDraft(new PartDraftDetailCondition(partNumber, revisionCode, draftKey)).preview());
    }

    @Operation(summary = "초기 DRAFT 상태의 부품 초안 대표 미리보기 비동기 처리 상태와 산출물 준비 여부를 조회합니다", description = "초기 DRAFT 상태의 부품 초안 대표 미리보기 비동기 처리 상태와 산출물 준비 여부를 조회합니다")
    @GetMapping("/{partNumber}/drafts/{draftKey}/preview/processing")
    public PartPreviewProcessingResponse getPreviewProcessing(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        return toPartPreviewProcessingResponse(
                partPreviewProcessingQuery.get(new PartPreviewProcessingCondition(partNumber, null, null, draftKey))
        );
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 비동기 처리 상태와 산출물 준비 여부를 조회합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 비동기 처리 상태와 산출물 준비 여부를 조회합니다")
    @GetMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/preview/processing")
    public PartPreviewProcessingResponse getPreviewProcessingFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        return toPartPreviewProcessingResponse(
                partPreviewProcessingQuery.get(new PartPreviewProcessingCondition(partNumber, null, revisionCode, draftKey))
        );
    }

    @Operation(summary = "초기 DRAFT 상태의 부품 초안 대표 미리보기를 해제합니다", description = "초기 DRAFT 상태의 부품 초안 대표 미리보기를 해제합니다")
    @DeleteMapping("/{partNumber}/drafts/{draftKey}/preview")
    public ResponseEntity<Void> deletePreview(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        clearPartPreviewUseCase.execute(new ClearPartPreviewCommand(partNumber, null, null, draftKey));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기를 해제합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기를 해제합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/preview")
    public ResponseEntity<Void> deletePreviewFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey
    ) {
        clearPartPreviewUseCase.execute(new ClearPartPreviewCommand(partNumber, null, revisionCode, draftKey));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "초기 DRAFT 상태의 부품 초안 대표 미리보기 전용 파일 1건을 삭제합니다", description = "초기 DRAFT 상태의 부품 초안 대표 미리보기 전용 파일 1건을 삭제합니다")
    @DeleteMapping("/{partNumber}/drafts/{draftKey}/preview/files/{previewFileId}")
    public ResponseEntity<Void> deletePreviewFile(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Parameter(description = "미리보기 파일 ID")
            @PathVariable UUID previewFileId
    ) {
        deletePartPreviewFileUseCase.execute(new DeletePartPreviewFileCommand(
                partNumber,
                null,
                null,
                draftKey,
                previewFileId
        ));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 전용 파일 1건을 삭제합니다", description = "특정 공식 리비전에서 파생된 DRAFT 상태의 부품 초안 대표 미리보기 전용 파일 1건을 삭제합니다")
    @DeleteMapping("/{partNumber}/revisions/{revisionCode}/drafts/{draftKey}/preview/files/{previewFileId}")
    public ResponseEntity<Void> deletePreviewFileFromRevision(
            @Parameter(description = "품번")
            @PathVariable String partNumber,
            @Parameter(description = "기준 리비전 코드")
            @PathVariable String revisionCode,
            @Parameter(description = "초안 키")
            @PathVariable String draftKey,
            @Parameter(description = "미리보기 파일 ID")
            @PathVariable UUID previewFileId
    ) {
        deletePartPreviewFileUseCase.execute(new DeletePartPreviewFileCommand(
                partNumber,
                null,
                revisionCode,
                draftKey,
                previewFileId
        ));
        return ResponseEntity.noContent().build();
    }
}
