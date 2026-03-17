package com.fabbitinc.server.presentation.engineeringchange.controller;

import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.presentation.workitem.dto.request.AttachFilesRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.CreateEngineeringChangeRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.SyncEngineeringChangeStepsRequest;
import com.fabbitinc.server.presentation.workitem.dto.request.CreateCommentRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.SyncPartRevisionsRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.SyncIssuesRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.request.UpdateEngineeringChangeRequest;
import com.fabbitinc.server.presentation.workitem.dto.request.UpdateCommentRequest;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeListResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangePartRevisionResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeLookupItemResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeLookupResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeStepResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.EngineeringChangeSummaryResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.CommentResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import com.fabbitinc.server.presentation.engineeringchange.dto.response.LinkedIssueSummaryResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.SyncDiffResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TeamBadgeResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TimelineItemResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.TimelineItemType;
import com.fabbitinc.server.presentation.workitem.dto.response.TimelineResponse;
import com.fabbitinc.server.application.engineeringchange.query.EngineeringChangeQuery;
import com.fabbitinc.server.application.engineeringchange.query.condition.EngineeringChangeDetailCondition;
import com.fabbitinc.server.application.engineeringchange.query.condition.EngineeringChangeListCondition;
import com.fabbitinc.server.application.engineeringchange.query.condition.EngineeringChangeLookupCondition;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeDetailResult;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeListResult;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeLookupResult;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangePartRevisionResult;
import com.fabbitinc.server.application.engineeringchange.query.result.EngineeringChangeStepResult;
import com.fabbitinc.server.application.workitem.query.result.FileItemResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineResult;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;
import com.fabbitinc.server.application.engineeringchange.query.result.LinkedIssueSummaryResult;
import com.fabbitinc.server.application.workitem.query.result.TeamBadgeResult;
import com.fabbitinc.server.application.workitem.query.result.TimelineItemTypeResult;
import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeReviewUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ApproveEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.AddEngineeringChangeFilesUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CancelEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CreateEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.CreateEngineeringChangeCommentUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.DeleteEngineeringChangeCommentUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.DeleteEngineeringChangeFileUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.RejectEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReleaseEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.ReplaceEngineeringChangeStepsUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SubmitEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncIssuesUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.SyncEngineeringChangePartRevisionsUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.UpdateEngineeringChangeUseCase;
import com.fabbitinc.server.application.engineeringchange.usecase.UpdateEngineeringChangeCommentUseCase;
import com.fabbitinc.server.application.workitem.usecase.result.AttachedFileResult;
import com.fabbitinc.server.application.workitem.usecase.result.CommentResult;
import com.fabbitinc.server.application.workitem.usecase.result.SyncDiffResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/engineering-changes")
@Tag(name = "engineering-changes", description = "변경관리 조회/생성/상태전이 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class EngineeringChangeController {

    private final EngineeringChangeQuery engineeringChangeQuery;
    private final CreateEngineeringChangeUseCase createEngineeringChangeUseCase;
    private final UpdateEngineeringChangeUseCase updateEngineeringChangeUseCase;
    private final SubmitEngineeringChangeUseCase submitEngineeringChangeUseCase;
    private final ApproveEngineeringChangeReviewUseCase approveEngineeringChangeReviewUseCase;
    private final RejectEngineeringChangeUseCase rejectEngineeringChangeUseCase;
    private final ApproveEngineeringChangeUseCase approveEngineeringChangeUseCase;
    private final ReleaseEngineeringChangeUseCase releaseEngineeringChangeUseCase;
    private final CancelEngineeringChangeUseCase cancelEngineeringChangeUseCase;
    private final SyncIssuesUseCase syncIssuesUseCase;
    private final ReplaceEngineeringChangeStepsUseCase replaceEngineeringChangeStepsUseCase;
    private final SyncEngineeringChangePartRevisionsUseCase syncEngineeringChangePartRevisionsUseCase;
    private final CreateEngineeringChangeCommentUseCase createEngineeringChangeCommentUseCase;
    private final UpdateEngineeringChangeCommentUseCase updateEngineeringChangeCommentUseCase;
    private final DeleteEngineeringChangeCommentUseCase deleteEngineeringChangeCommentUseCase;
    private final AddEngineeringChangeFilesUseCase addEngineeringChangeFilesUseCase;
    private final DeleteEngineeringChangeFileUseCase deleteEngineeringChangeFileUseCase;

    @Operation(
            summary = "변경관리 목록을 조회합니다",
            description = "변경관리 목록을 조회합니다"
    )
    @GetMapping
    public EngineeringChangeListResponse listEngineeringChanges(
            @Parameter(description = "변경관리 제목 검색어", example = "품번")
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "offset", defaultValue = "0")
            @Min(value = 0, message = "offset은 0 이상이어야 합니다") int offset,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 100, message = "limit은 100 이하여야 합니다") int limit
    ) {
        return toEngineeringChangeListResponse(
                engineeringChangeQuery.listEngineeringChanges(new EngineeringChangeListCondition(search, state, offset, limit))
        );
    }

    @Operation(
            summary = "변경관리 연결 picker UI용 경량 목록을 조회합니다",
            description = "변경관리 연결 picker UI용 경량 목록을 조회합니다"
    )
    @GetMapping("/lookup")
    public EngineeringChangeLookupResponse lookupEngineeringChanges(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit
    ) {
        return toEngineeringChangeLookupResponse(
                engineeringChangeQuery.lookupEngineeringChanges(new EngineeringChangeLookupCondition(search, limit))
        );
    }

    @Operation(
            summary = "변경관리 상세 정보를 조회합니다",
            description = "변경관리 ID로 상세 정보를 조회합니다"
    )
    @GetMapping("/{engineeringChangeId}")
    public EngineeringChangeResponse getEngineeringChange(
            @Parameter(description = "조회할 변경관리 ID")
            @PathVariable UUID engineeringChangeId
    ) {
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId))
        );
    }

    @Operation(
            summary = "변경관리를 생성하고 연관 정보(이슈/부품 리비전/단계/파일)를 일괄 연결합니다",
            description = "변경관리를 생성하고 연관 정보(이슈/부품 리비전/단계/파일)를 일괄 연결합니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EngineeringChangeResponse createEngineeringChange(
            @Parameter(description = "변경관리 생성 요청")
            @Valid @RequestBody CreateEngineeringChangeRequest request
    ) {
        CreateEngineeringChangeUseCase.CreateEngineeringChangeResult result = createEngineeringChangeUseCase.execute(
                new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand(
                        request.title(),
                        request.body(),
                        request.sourceIssueId(),
                        request.partRevisions().stream()
                                .map(item -> new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.PartRevisionTarget(
                                        item.revisionId()
                        ))
                                .toList(),
                        request.fileIds(),
                        request.steps().stream()
                                .map(step -> new CreateEngineeringChangeUseCase.CreateEngineeringChangeCommand.StepTarget(
                                        step.stepType(),
                                        step.assigneeType(),
                                        step.assigneeId(),
                                        step.sequence()
                                ))
                                .toList()
                )
        );
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(result.engineeringChangeId()))
        );
    }

    @Operation(
            summary = "변경관리 제목/본문을 수정합니다",
            description = "변경관리 제목/본문을 수정합니다"
    )
    @PatchMapping("/{engineeringChangeId}")
    public EngineeringChangeResponse updateEngineeringChange(
            @Parameter(description = "수정할 변경관리 ID")
            @PathVariable UUID engineeringChangeId,
            @Parameter(description = "변경관리 수정 요청")
            @Valid @RequestBody UpdateEngineeringChangeRequest request
    ) {
        updateEngineeringChangeUseCase.execute(
                new UpdateEngineeringChangeUseCase.UpdateEngineeringChangeCommand(
                        engineeringChangeId,
                        request.title(),
                        request.body(),
                        request.steps() == null ? null : request.steps().stream()
                                .map(step -> new UpdateEngineeringChangeUseCase.UpdateEngineeringChangeCommand.StepTarget(
                                        step.stepType(),
                                        step.assigneeType(),
                                        step.assigneeId(),
                                        step.sequence()
                                ))
                                .toList()
                )
        );
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId))
        );
    }

    @Operation(
            summary = "변경관리를 검토 대기로 전환합니다 (DRAFT -> REVIEW_PENDING)",
            description = "변경관리를 검토 대기로 전환합니다 (DRAFT -> REVIEW_PENDING)"
    )
    @PostMapping("/{engineeringChangeId}/submit")
    public EngineeringChangeResponse submit(
            @PathVariable UUID engineeringChangeId
    ) {
        submitEngineeringChangeUseCase.execute(new SubmitEngineeringChangeUseCase.SubmitEngineeringChangeCommand(engineeringChangeId));
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId))
        );
    }

    @Operation(
            summary = "변경관리를 작성 단계로 되돌립니다 (*_PENDING -> DRAFT)",
            description = "변경관리를 작성 단계로 되돌립니다 (*_PENDING -> DRAFT)"
    )
    @PostMapping("/{engineeringChangeId}/reject")
    public EngineeringChangeResponse reject(
            @PathVariable UUID engineeringChangeId
    ) {
        rejectEngineeringChangeUseCase.execute(
                new RejectEngineeringChangeUseCase.RejectEngineeringChangeCommand(engineeringChangeId)
        );
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId))
        );
    }

    @Operation(
            summary = "현재 승인 단계 담당자가 변경관리를 승인해 반영 대기로 전환합니다 (APPROVAL_PENDING -> RELEASE_PENDING)",
            description = "현재 승인 단계 담당자가 변경관리를 승인해 반영 대기로 전환합니다 (APPROVAL_PENDING -> RELEASE_PENDING)"
    )
    @PostMapping("/{engineeringChangeId}/approve")
    public EngineeringChangeResponse approve(
            @PathVariable UUID engineeringChangeId
    ) {
        approveEngineeringChangeUseCase.execute(
                new ApproveEngineeringChangeUseCase.ApproveEngineeringChangeCommand(engineeringChangeId)
        );
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId))
        );
    }

    @Operation(
            summary = "현재 반영 단계 담당자가 변경관리를 반영 완료합니다 (RELEASE_PENDING -> RELEASED)",
            description = "현재 반영 단계 담당자가 변경관리를 반영 완료합니다 (RELEASE_PENDING -> RELEASED)"
    )
    @PostMapping("/{engineeringChangeId}/release")
    public EngineeringChangeResponse release(
            @PathVariable UUID engineeringChangeId
    ) {
        releaseEngineeringChangeUseCase.execute(
                new ReleaseEngineeringChangeUseCase.ReleaseEngineeringChangeCommand(engineeringChangeId)
        );
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId))
        );
    }

    @Operation(
            summary = "변경관리를 폐기하고 미반영 리비전을 취소합니다",
            description = "변경관리를 폐기하고 미반영 리비전을 취소합니다"
    )
    @PostMapping("/{engineeringChangeId}/cancel")
    public EngineeringChangeResponse cancel(
            @PathVariable UUID engineeringChangeId
    ) {
        cancelEngineeringChangeUseCase.execute(
                new CancelEngineeringChangeUseCase.CancelEngineeringChangeCommand(engineeringChangeId)
        );
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId))
        );
    }

    @Operation(
            summary = "변경관리에 연결된 이슈 목록을 동기화합니다",
            description = "변경관리에 연결된 이슈 목록을 동기화합니다"
    )
    @PutMapping("/{engineeringChangeId}/issues")
    public SyncDiffResponse syncIssues(
            @PathVariable UUID engineeringChangeId,
            @Valid @RequestBody SyncIssuesRequest request
    ) {
        return toSyncDiffResponse(
                syncIssuesUseCase.execute(new SyncIssuesUseCase.SyncIssuesCommand(engineeringChangeId, request.issueIds()))
        );
    }

    @Operation(
            summary = "변경관리 단계 목록을 동기화합니다",
            description = "변경관리 단계 목록을 동기화합니다"
    )
    @PutMapping("/{engineeringChangeId}/steps")
    public EngineeringChangeResponse replaceSteps(
            @PathVariable UUID engineeringChangeId,
            @Valid @RequestBody SyncEngineeringChangeStepsRequest request
    ) {
        replaceEngineeringChangeStepsUseCase.execute(
                new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand(
                        engineeringChangeId,
                        request.steps().stream()
                                .map(step -> new ReplaceEngineeringChangeStepsUseCase.ReplaceEngineeringChangeStepsCommand.Item(
                                        step.stepType(),
                                        step.assigneeType(),
                                        step.assigneeId(),
                                        step.sequence()
                                ))
                                .toList()
                )
        );
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId))
        );
    }

    @Operation(
            summary = "현재 검토 단계 담당자가 자신의 검토 단계를 승인합니다",
            description = "현재 검토 단계 담당자가 자신의 검토 단계를 승인합니다"
    )
    @PostMapping("/{engineeringChangeId}/review/approve")
    public EngineeringChangeResponse approveReview(
            @PathVariable UUID engineeringChangeId
    ) {
        approveEngineeringChangeReviewUseCase.execute(
                new ApproveEngineeringChangeReviewUseCase.ApproveEngineeringChangeReviewCommand(engineeringChangeId)
        );
        return toEngineeringChangeResponse(
                engineeringChangeQuery.getEngineeringChange(new EngineeringChangeDetailCondition(engineeringChangeId))
        );
    }

    @Operation(
            summary = "변경관리에 연결할 부품 초안 목록을 동기화합니다",
            description = "변경관리에 연결할 부품 초안 목록을 동기화합니다"
    )
    @PutMapping("/{engineeringChangeId}/part-revisions")
    public SyncDiffResponse syncPartRevisions(
            @PathVariable UUID engineeringChangeId,
            @Valid @RequestBody SyncPartRevisionsRequest request
    ) {
        return toSyncDiffResponse(
                syncEngineeringChangePartRevisionsUseCase.execute(
                        new SyncEngineeringChangePartRevisionsUseCase.SyncEngineeringChangePartRevisionsCommand(
                                engineeringChangeId,
                                request.items().stream()
                                        .map(item -> new SyncEngineeringChangePartRevisionsUseCase.SyncEngineeringChangePartRevisionsCommand.Item(
                                                item.revisionId()
                                        ))
                                        .toList()
                        )
                )
        );
    }

    @Operation(
            summary = "댓글과 활동 이력을 시간순으로 병합 조회합니다",
            description = "댓글과 활동 이력을 시간순으로 병합 조회합니다"
    )
    @GetMapping("/{engineeringChangeId}/timeline")
    public TimelineResponse getTimeline(
            @PathVariable UUID engineeringChangeId
    ) {
        return toTimelineResponse(engineeringChangeQuery.getTimeline(engineeringChangeId));
    }

    @Operation(
            summary = "댓글을 생성합니다",
            description = "댓글을 생성합니다"
    )
    @PostMapping("/{engineeringChangeId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable UUID engineeringChangeId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return toCommentResponse(
                createEngineeringChangeCommentUseCase.execute(
                        new CreateEngineeringChangeCommentUseCase.CreateEngineeringChangeCommentCommand(
                                engineeringChangeId,
                                request.body()
                        )
                )
        );
    }

    @Operation(
            summary = "댓글을 수정합니다",
            description = "댓글을 수정합니다"
    )
    @PatchMapping("/{engineeringChangeId}/comments/{commentId}")
    public CommentResponse updateComment(
            @PathVariable UUID engineeringChangeId,
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return toCommentResponse(
                updateEngineeringChangeCommentUseCase.execute(
                        new UpdateEngineeringChangeCommentUseCase.UpdateEngineeringChangeCommentCommand(
                                engineeringChangeId,
                                commentId,
                                request.body()
                        )
                )
        );
    }

    @Operation(
            summary = "댓글을 삭제합니다",
            description = "댓글을 삭제합니다"
    )
    @DeleteMapping("/{engineeringChangeId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID engineeringChangeId,
            @PathVariable UUID commentId
    ) {
        deleteEngineeringChangeCommentUseCase.execute(
                new DeleteEngineeringChangeCommentUseCase.DeleteEngineeringChangeCommentCommand(engineeringChangeId, commentId)
        );
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "첨부파일을 배치 연결합니다",
            description = "첨부파일을 배치 연결합니다"
    )
    @PostMapping("/{engineeringChangeId}/files")
    public List<FileItemResponse> addFiles(
            @PathVariable UUID engineeringChangeId,
            @Valid @RequestBody AttachFilesRequest request
    ) {
        return addEngineeringChangeFilesUseCase.execute(
                        new AddEngineeringChangeFilesUseCase.AddEngineeringChangeFilesCommand(
                                engineeringChangeId,
                                request.fileIds()
                        )
                )
                .stream()
                .map(this::toFileItemResponse)
                .toList();
    }

    @Operation(
            summary = "첨부파일 1건을 삭제(soft delete)합니다",
            description = "첨부파일 1건을 삭제(soft delete)합니다"
    )
    @DeleteMapping("/{engineeringChangeId}/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable UUID engineeringChangeId,
            @PathVariable UUID fileId
    ) {
        deleteEngineeringChangeFileUseCase.execute(
                new DeleteEngineeringChangeFileUseCase.DeleteEngineeringChangeFileCommand(engineeringChangeId, fileId)
        );
        return ResponseEntity.noContent().build();
    }

    private SyncDiffResponse toSyncDiffResponse(SyncDiffResult result) {
        return new SyncDiffResponse(result.addedCount(), result.removedCount());
    }

    private CommentResponse toCommentResponse(CommentResult result) {
        return new CommentResponse(
                result.id(),
                result.targetId(),
                result.body(),
                result.createdAt(),
                result.updatedAt(),
                result.isModified(),
                result.createdBy()
        );
    }

    private EngineeringChangeListResponse toEngineeringChangeListResponse(EngineeringChangeListResult result) {
        return new EngineeringChangeListResponse(
                result.openCount(),
                result.closedCount(),
                result.total(),
                result.offset(),
                result.limit(),
                result.items().stream().map(this::toEngineeringChangeSummaryResponse).toList()
        );
    }

    private EngineeringChangeSummaryResponse toEngineeringChangeSummaryResponse(EngineeringChangeListResult.Item result) {
        return new EngineeringChangeSummaryResponse(
                result.id(),
                result.number(),
                result.title(),
                result.state(),
                result.closedAt(),
                result.createdAt(),
                result.updatedAt(),
                toUserSummaryResponse(result.createdBy()),
                result.steps().stream().map(this::toEngineeringChangeStepResponse).toList(),
                result.files().stream().map(this::toFileItemResponse).toList(),
                result.commentsCount(),
                result.releasedAt(),
                toUserSummaryResponse(result.releasedBy())
        );
    }

    private EngineeringChangeLookupResponse toEngineeringChangeLookupResponse(EngineeringChangeLookupResult result) {
        return new EngineeringChangeLookupResponse(
                result.items().stream()
                        .map(item -> new EngineeringChangeLookupItemResponse(
                                item.id(),
                                item.number(),
                                item.title(),
                                item.state()
                        ))
                        .toList()
        );
    }

    private EngineeringChangeResponse toEngineeringChangeResponse(EngineeringChangeDetailResult result) {
        return new EngineeringChangeResponse(
                result.id(),
                result.number(),
                result.title(),
                result.body(),
                result.state(),
                result.closedAt(),
                result.createdAt(),
                result.updatedAt(),
                result.isModified(),
                toUserSummaryResponse(result.createdBy()),
                toLinkedIssueSummaryResponse(result.sourceIssue()),
                result.steps().stream().map(this::toEngineeringChangeStepResponse).toList(),
                result.partRevisions().stream().map(this::toPartRevisionResponse).toList(),
                result.files().stream().map(this::toFileItemResponse).toList(),
                result.commentsCount(),
                result.releasedAt(),
                toUserSummaryResponse(result.releasedBy()),
                result.linkedIssues().stream().map(this::toLinkedIssueSummaryResponse).toList()
        );
    }

    private TimelineResponse toTimelineResponse(TimelineResult result) {
        Map<String, UserSummaryResponse> users = new LinkedHashMap<>();
        result.users().forEach((userId, user) -> users.put(userId, toUserSummaryResponse(user)));

        return new TimelineResponse(
                result.items().stream().map(this::toTimelineItemResponse).toList(),
                users
        );
    }

    private TimelineItemResponse toTimelineItemResponse(TimelineResult.Item result) {
        return new TimelineItemResponse(
                toTimelineItemType(result.type()),
                result.id(),
                result.action(),
                result.scope(),
                result.actorId(),
                result.detail(),
                result.body(),
                result.authorId(),
                result.createdAt(),
                result.updatedAt(),
                result.isModified()
        );
    }

    private TimelineItemType toTimelineItemType(TimelineItemTypeResult result) {
        if (result == TimelineItemTypeResult.ACTIVITY) {
            return TimelineItemType.ACTIVITY;
        }
        return TimelineItemType.COMMENT;
    }

    private UserSummaryResponse toUserSummaryResponse(UserSummaryResult result) {
        if (result == null) {
            return null;
        }
        return new UserSummaryResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.phone(),
                result.profileImageUrl()
        );
    }

    private TeamBadgeResponse toTeamBadgeResponse(TeamBadgeResult result) {
        return new TeamBadgeResponse(result.id(), result.name());
    }

    private EngineeringChangePartRevisionResponse toPartRevisionResponse(EngineeringChangePartRevisionResult result) {
        return new EngineeringChangePartRevisionResponse(
                result.revisionId(),
                result.partId(),
                result.partNumber(),
                result.baseRevisionCode(),
                result.revisionCode(),
                result.name(),
                result.status()
        );
    }

    private EngineeringChangeStepResponse toEngineeringChangeStepResponse(EngineeringChangeStepResult result) {
        return new EngineeringChangeStepResponse(
                result.stepId(),
                result.stepType(),
                result.assigneeType(),
                result.sequence(),
                result.status(),
                toUserSummaryResponse(result.assigneeUser()),
                toTeamBadgeResponse(result.assigneeTeam()),
                toUserSummaryResponse(result.actedBy()),
                result.actedAt()
        );
    }

    private FileItemResponse toFileItemResponse(FileItemResult result) {
        return new FileItemResponse(
                result.fileId(),
                result.originalName(),
                result.contentType(),
                result.fileSize(),
                result.fileUrl(),
                result.createdAt()
        );
    }

    private FileItemResponse toFileItemResponse(AttachedFileResult result) {
        return new FileItemResponse(
                result.fileId(),
                result.originalName(),
                result.contentType(),
                result.fileSize(),
                result.fileUrl(),
                result.createdAt()
        );
    }

    private LinkedIssueSummaryResponse toLinkedIssueSummaryResponse(LinkedIssueSummaryResult result) {
        return new LinkedIssueSummaryResponse(
                result.id(),
                result.number(),
                result.title(),
                result.state()
        );
    }
}
